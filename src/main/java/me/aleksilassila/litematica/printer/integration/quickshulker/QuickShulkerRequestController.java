package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.utils.mods.ModLoadUtils;
import me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.utils.mods.TakeItOutUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Collection;

public final class QuickShulkerRequestController {

    private final Minecraft client;
    private final OrderedStorageController orderedStorage;
    private final QuickShulkerContainerSession containerSession = new QuickShulkerContainerSession();
    private int shulkerCooldown = 0;
    private int openHandlerTimeout = 0;
    private static final int OPEN_HANDLER_BASE_TIMEOUT_TICKS = 8;
    private static final int OPEN_HANDLER_SAFETY_TICKS = 2;
    private static final int OPEN_HANDLER_MAX_TIMEOUT_TICKS = 40;

    private final HashSet<Item> lastNeedItemList = new LinkedHashSet<>();
    private boolean isOpenHandler;
    private boolean emptyShulkerOnly;
    private boolean externalRequestAllowed;
    private int shulkerInventoryMenuSlot = -1;
    private int automatedContainerId = -1;
    private final ConfirmedInventorySwap transfer = new ConfirmedInventorySwap();
    private final InventorySyncDrain cancellation = new InventorySyncDrain();
    private AbstractContainerMenu transferMenu;
    private Object transferConnection;
    private boolean clearingHotbar;
    private int targetHotbarSlot = -1;
    private int movedInventorySlot = -1;
    private int extractionSlot = -1;
    private ItemStack extractedStack = ItemStack.EMPTY;
    private ItemStack sourceShulker = ItemStack.EMPTY;

    public QuickShulkerRequestController(Minecraft client) {
        this.client = client;
        this.orderedStorage = new OrderedStorageController(client, () -> this.lastNeedItemList, this.containerSession);
    }

    OrderedStorageController orderedStorage() {
        return this.orderedStorage;
    }

    public void requestItem(Item item) {
        if (item != null) {
            lastNeedItemList.add(item);
        }
    }

    public void requestItems(Collection<Item> items) {
        requestItems(items, false);
    }

    public void requestItems(Collection<Item> items, boolean emptyShulkerOnly) {
        this.emptyShulkerOnly = emptyShulkerOnly;
        if (items == null) return;
        for (Item item : items) {
            this.requestItem(item);
        }
    }

    public boolean isOpenHandler() {
        return this.isOpenHandler;
    }

    public boolean switchItem() {
        if (this.cancellation.active()) return true;
        if (!this.lastNeedItemList.isEmpty() && !this.isOpenHandler) {
            LocalPlayer player = client.player;
            if (player == null) {
                clearSwitchRequest();
                return false;
            }
            AbstractContainerMenu sc = player.containerMenu;
            if (!sc.equals(player.inventoryMenu)) return true;
            if (Configs.Placement.STORE_ORDERLY.getBooleanValue()
                    && Configs.Placement.QUICK_SHULKER.getBooleanValue()
                    && this.orderedStorage.tryRestoreForInventoryPressure()) {
                return true;
            }

            if (Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
                if (shulkerCooldown > 0) {
                    return true;
                }
                if (this.openShulker(this.lastNeedItemList)) {
                    return true;
                }
            }
            clearSwitchRequest();
        }
        return false;
    }

    public boolean hasPendingSwitchRequest() {
        return this.cancellation.active() || ShulkerUtils.isRecovering() || this.isOpenHandler || !this.lastNeedItemList.isEmpty() || this.orderedStorage.hasPendingRestore();
    }

    public boolean shouldPauseForSwitchRequest() {
        return (Configs.Core.WORK_SWITCH.getBooleanValue() || this.externalRequestAllowed)
                && Configs.Placement.QUICK_SHULKER.getBooleanValue()
                && hasPendingSwitchRequest();
    }

    public void onContainerOpen(int containerId, net.minecraft.world.inventory.MenuType<?> type) {
        if (this.isOpenHandler || this.orderedStorage.isWaitingForRestoreContainer()) {
            this.containerSession.opened(containerId, type);
            QuickShulkerDiagnostics.log("open-candidate menu=" + containerId + " type=" + type);
        }
    }

    public boolean confirmContainer(int containerId, java.util.List<ItemStack> items, ItemStack carried) {
        if (!this.isOpenHandler && !this.orderedStorage.isWaitingForRestoreContainer()) return false;
        if (this.containerSession.owns(containerId)) return true;
        if (!this.containerSession.isCandidate(containerId)) return false;
        if (!this.containerSession.confirm(containerId, items, carried)) {
            QuickShulkerDiagnostics.log("open-rejected contents differ from source box menu=" + containerId);
            // This is not our container: do not hide it, click it, close it or discard cursor ownership.
            this.orderedStorage.reset();
            clearSwitchRequest();
            this.containerSession.reset();
            ShulkerUtils.recoverFailedOpen();
            return false;
        }
        this.automatedContainerId = containerId;
        this.orderedStorage.onContainerOpen(containerId);
        ShulkerUtils.clearOpenAttempt();
        ModLoadUtils.closeScreen = 0;
        QuickShulkerDiagnostics.log("open-confirmed menu=" + containerId);
        // Only hide the screen after the server contents identify the expected box.
        // Removing this screen does not send a container-close packet (unlike onClose()).
        //#if MC > 260100
        //$$ var screen = client.gui.screen();
        //#else
        var screen = client.screen;
        //#endif
        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container
                && container.getMenu().containerId == containerId) {
            //#if MC > 260100
            //$$ client.gui.setScreen(null);
            //#else
            client.setScreen(null);
            //#endif
        }
        return true;
    }

    public boolean shouldSuppressContainerScreen(int containerId) {
        LocalPlayer player = client.player;
        return (Configs.Core.WORK_SWITCH.getBooleanValue() || this.externalRequestAllowed)
                && Configs.Placement.QUICK_SHULKER.getBooleanValue()
                && player != null
                && !player.containerMenu.equals(player.inventoryMenu)
                && containerId == this.automatedContainerId
                && (this.isOpenHandler || this.orderedStorage.isWaitingForRestoreContainer());
    }

    public void setExternalRequestAllowed(boolean allowed) {
        this.externalRequestAllowed = allowed;
    }

    public void resetRuntime() {
        if (this.transfer.active() && client != null && client.player != null) {
            this.cancellation.begin(this.transferMenu, this.transferConnection);
        }
        if (!this.cancellation.active()) closeAutomatedContainer();
        ShulkerUtils.recoverFailedOpen();
        clearSwitchRequest();
        this.externalRequestAllowed = false;
        shulkerCooldown = 0;
        ModLoadUtils.closeScreen = 0;
        tickCancellation();
    }

    public boolean isCancelling() { return this.cancellation.active(); }

    /** A new world/connection cannot inherit either a menu ID or a queued cleanup click. */
    public void discardForClientScopeChange() {
        this.cancellation.reset();
        clearSwitchRequest();
    }

    public void tickCancellation() {
        if (!this.cancellation.active()) return;
        var menu = client == null || client.player == null ? null : client.player.containerMenu;
        var connection = client == null ? null : client.getConnection();
        if (this.cancellation.needsRequest(menu, connection)) {
            this.cancellation.requested(menu);
            QuickShulkerDiagnostics.log("cancel-sync-request menu=" + menu.containerId);
            QuickShulkerClicks.requestFullSync(menu);
        }
    }

    public boolean finishCancellation(int containerId, int size) {
        if (!this.cancellation.active() || client == null || client.player == null) return false;
        var menu = client.player.containerMenu;
        if (!this.cancellation.accepts(menu, client.getConnection(), containerId, size)) return false;
        boolean close = this.cancellation.owns(menu) && menu != client.player.inventoryMenu;
        this.cancellation.reset();
        QuickShulkerDiagnostics.log("cancel-synchronized menu=" + containerId);
        if (close) client.player.closeContainer();
        return true;
    }

    public void switchInv() {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || transfer.active()) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu || menu.containerId != automatedContainerId
                || !menu.getCarried().isEmpty()) return;
        for (int source = 0; source < menu.slots.size(); source++) {
            Slot slot = menu.slots.get(source);
            if (slot.container == player.getInventory() || !lastNeedItemList.contains(slot.getItem().getItem())
                    || this.emptyShulkerOnly && !me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest.isEmptyShulker(slot.getItem())) continue;
            for (String configured : fi.dy.masa.litematica.config.Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue().split(",")) {
                int hotbar;
                try { hotbar = Integer.parseInt(configured.trim()) - 1; }
                catch (NumberFormatException ignored) { continue; }
                if (hotbar < 0 || hotbar >= 9) continue;
                if (!me.aleksilassila.litematica.printer.mixin.printer.litematica.InventoryUtilsAccessor
                        .canPickToSlot(player.getInventory(), hotbar)) continue;
                // Validate the final destination, not a configured slot which is subsequently replaced.
                if (!QuickShulkerSupport.isSafeHotbarTarget(hotbar, shulkerInventoryMenuSlot,
                        player.getInventory().getItem(hotbar))) continue;
                int target = findPlayerSlot(menu, hotbar);
                if (target < 0) continue;
                targetHotbarSlot = hotbar;
                if (!menu.slots.get(target).getItem().isEmpty()) {
                    int empty = -1;
                    for (int i = 0; i < menu.slots.size(); i++) {
                        Slot candidate = menu.slots.get(i);
                        if (candidate.container == player.getInventory() && candidate.getContainerSlot() >= 9
                                && candidate.getContainerSlot() < 36 && candidate.getItem().isEmpty()) {
                            empty = i;
                            break;
                        }
                    }
                    if (empty < 0) continue;
                    clearingHotbar = true;
                    movedInventorySlot = menu.slots.get(empty).getContainerSlot();
                    beginSwap(menu, empty, target, hotbar);
                    return;
                }
                clearingHotbar = false;
                extractionSlot = source;
                extractedStack = slot.getItem().copy();
                sourceShulker = player.inventoryMenu.slots.get(shulkerInventoryMenuSlot).getItem().copy();
                beginSwap(menu, source, target, hotbar);
                return;
            }
        }
        closeAutomatedContainer();
        clearSwitchRequest();
    }

    private static int findPlayerSlot(AbstractContainerMenu menu, int inventorySlot) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory
                    && slot.getContainerSlot() == inventorySlot) return i;
        }
        return -1;
    }

    private void beginSwap(AbstractContainerMenu menu, int source, int target, int hotbar) {
        this.transferMenu = menu;
        this.transferConnection = client.getConnection();
        transfer.begin(menu.containerId, source, target,
                menu.slots.get(source).getItem(), menu.slots.get(target).getItem());
        openHandlerTimeout = OPEN_HANDLER_MAX_TIMEOUT_TICKS;
        QuickShulkerDiagnostics.log("swap-send menu=" + menu.containerId + " revision=" + menu.getStateId()
                + " source=" + source + " target=" + target + " hotbar=" + hotbar
                + " sourceStack=" + QuickShulkerDiagnostics.stack(menu.slots.get(source).getItem())
                + " targetStack=" + QuickShulkerDiagnostics.stack(menu.slots.get(target).getItem()));
        QuickShulkerClicks.send(menu, source, hotbar, ContainerInput.SWAP);
    }

    public void onSlotUpdate(int containerId, int slot, ItemStack stack) {
        if (transfer.active()) {
            QuickShulkerDiagnostics.log("slot-reply menu=" + containerId + " slot=" + slot
                    + " stack=" + QuickShulkerDiagnostics.stack(stack));
            transfer.observe(containerId, slot, stack);
        }
    }

    private void completeTransfer() {
        if (!transfer.confirmed()) return;
        LocalPlayer player = client.player;
        if (player == null || player.containerMenu.containerId != automatedContainerId
                || !player.containerMenu.getCarried().isEmpty()) return;
        transfer.reset();
        QuickShulkerDiagnostics.log("swap-confirmed menu=" + automatedContainerId + " clearing=" + clearingHotbar);
        if (clearingHotbar) {
            orderedStorage.moveTrackedItem(targetHotbarSlot, movedInventorySlot);
            switchInv();
            return;
        }
        orderedStorage.newItem(extractedStack, sourceShulker, extractionSlot,
                shulkerInventoryMenuSlot, targetHotbarSlot);
        me.aleksilassila.litematica.printer.utils.InventoryUtils.setSelectedSlot(player.getInventory(), targetHotbarSlot);
        me.aleksilassila.litematica.printer.utils.InventoryUtils.syncSelectedHotbarSlot();
        RuntimeAccess.get().inventorySwitchGuard().markSwitchIfNeeded(extractedStack.getItem());
        closeAutomatedContainer();
        clearSwitchRequest();
    }

    private boolean openShulker(HashSet<Item> items) {
        if (shulkerCooldown > 0) {
            return false;
        }
        for (Item item : items) {
            AbstractContainerMenu sc = Minecraft.getInstance().player.inventoryMenu;
            for (int i = 9; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getItem();
                String itemid = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (itemid.contains("shulker_box") && stack.getCount() == 1) {
                    var items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if (items1.stream().anyMatch(s1 -> s1.getItem().equals(item)
                            && (!this.emptyShulkerOnly || me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest.isEmptyShulker(s1)))) {
                        try {
                            shulkerInventoryMenuSlot = i;
                            QuickShulkerDiagnostics.log("open-send source=" + i + " wanted=" + item
                                    + " mode=" + Configs.Placement.QUICK_SHULKER_MODE.getOptionListValue());
                            this.containerSession.begin(stack, Configs.Placement.QUICK_SHULKER_MODE.getOptionListValue()
                                    == me.aleksilassila.litematica.printer.enums.QuickShulkerModeType.CLICK_SLOT);
                            if (!ShulkerUtils.openShulker(stack, shulkerInventoryMenuSlot)) {
                                this.containerSession.reset();
                                shulkerInventoryMenuSlot = -1;
                                continue;
                            }
                            ModLoadUtils.closeScreen++;
                            this.isOpenHandler = true;
                            openHandlerTimeout = RuntimeAccess.get().rttReplayController().getWaitTimeoutTicks(
                                    OPEN_HANDLER_BASE_TIMEOUT_TICKS,
                                    OPEN_HANDLER_SAFETY_TICKS,
                                    OPEN_HANDLER_MAX_TIMEOUT_TICKS
                            );
                            shulkerCooldown = Configs.Placement.QUICK_SHULKER_COOLDOWN.getIntegerValue();
                            return true;
                        } catch (Exception e) {
                            Reference.LOGGER.warn("Quick Shulker open failed at inventory slot {}", i, e);
                        }
                    }
                }
            }
        }
        return false;
    }

    public void tick() {
        if (this.cancellation.active()) {
            tickCancellation();
            return;
        }
        if (!Configs.Placement.STORE_ORDERLY.getBooleanValue() && this.orderedStorage.hasPendingRestore()) {
            if (!this.isOpenHandler) closeAutomatedContainer();
            this.orderedStorage.reset();
            ModLoadUtils.closeScreen = 0;
            ShulkerUtils.recoverFailedOpen();
        }
        this.orderedStorage.tick();
        completeTransfer();
        if (!this.isOpenHandler && !this.orderedStorage.isWaitingForRestoreContainer()) {
            this.automatedContainerId = -1;
            this.containerSession.reset();
        }
        if (this.isOpenHandler && this.openHandlerTimeout > 0 && --this.openHandlerTimeout <= 0) {
            QuickShulkerDiagnostics.log("timeout menu=" + automatedContainerId + " transfer=" + transfer.active());
            resetRuntime();
            return;
        }
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }
        if (Configs.Placement.STORE_ORDERLY.getBooleanValue()
                && Configs.Placement.QUICK_SHULKER.getBooleanValue()
                && !this.externalRequestAllowed
                && !this.isOpenHandler
                && !RuntimeAccess.get().inventorySwitchGuard().isWaiting()
                && !TakeItOutUtils.isAwaitingStack()) {
            this.orderedStorage.maintainOrderlyStorage();
        }
    }

    private void clearSwitchRequest() {
        this.transfer.reset();
        this.transferMenu = null;
        this.transferConnection = null;
        this.extractedStack = ItemStack.EMPTY;
        this.sourceShulker = ItemStack.EMPTY;
        this.shulkerInventoryMenuSlot = -1;
        this.lastNeedItemList.clear();
        this.emptyShulkerOnly = false;
        this.isOpenHandler = false;
        this.openHandlerTimeout = 0;
        if (!this.orderedStorage.isWaitingForRestoreContainer()) {
            ModLoadUtils.closeScreen = 0;
            this.automatedContainerId = -1;
            this.containerSession.reset();
        }
    }

    private void closeAutomatedContainer() {
        LocalPlayer player = this.client == null ? null : this.client.player;
        if (player != null
                && !player.containerMenu.equals(player.inventoryMenu)
                && player.containerMenu.containerId == this.automatedContainerId) {
            player.closeContainer();
        }
    }
}
