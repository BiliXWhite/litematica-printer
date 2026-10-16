package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils;
import me.aleksilassila.litematica.printer.utils.mods.ModLoadUtils;
import me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

final class OrderedStorageController {
    private int automatedContainerId = -1;
    private static final int RESTORE_TIMEOUT_TICKS = 40;
    private static final int PRESSURE_TRIGGER_FREE_SLOTS = 4;
    private static final int PRESSURE_TARGET_FREE_SLOTS = 8;
    private static final int EMERGENCY_FREE_SLOTS = 1;
    private static final int RECENT_USE_PROTECTION_TICKS = 40;
    private static final int IDLE_RESTORE_DELAY_TICKS = 100;
    private final Minecraft client;
    private final QuickShulkerContainerSession containerSession;
    private final Supplier<Set<Item>> neededItems;
    private final List<OrderedStorageEntry> trackedItems = new ArrayList<>();
    private final RestoreSession<OrderedStorageEntry> restoreSession = new RestoreSession<>();

    OrderedStorageController(Minecraft client, Supplier<Set<Item>> neededItems, QuickShulkerContainerSession containerSession) {
        this.client = client;
        this.containerSession = containerSession;
        this.neededItems = neededItems;
    }

    public void newItem(
            ItemStack itemStack,
            ItemStack sourceShulker,
            int sourceContainerSlot,
            int shulkerInventoryMenuSlot,
            int playerInventorySlot
    ) {
        if (itemStack == null || itemStack.isEmpty()
                || sourceContainerSlot < 0
                || shulkerInventoryMenuSlot < 0
                || playerInventorySlot < 0
                || playerInventorySlot >= 36) {
            return;
        }
        this.trackedItems.removeIf(statistics -> statistics.playerInventorySlot == playerInventorySlot);
        this.trackedItems.add(new OrderedStorageEntry(
                itemStack,
                sourceShulker,
                sourceContainerSlot,
                shulkerInventoryMenuSlot,
                playerInventorySlot,
                currentGameTick()
        ));
        markPrinterActivity();
    }

    public void moveTrackedItem(int oldPlayerSlot, int newPlayerSlot) {
        OrderedStorageEntry moved = null;
        for (OrderedStorageEntry statistics : this.trackedItems) {
            if (statistics.playerInventorySlot == oldPlayerSlot) {
                moved = statistics;
                break;
            }
        }
        if (moved == null) {
            return;
        }
        if (newPlayerSlot < 0 || newPlayerSlot >= 36) {
            this.trackedItems.remove(moved);
            if (this.restoreSession.pending() == moved) {
                clearPendingRestore();
            }
            return;
        }
        OrderedStorageEntry movedRecord = moved;
        this.trackedItems.removeIf(statistics -> statistics != movedRecord
                && statistics.playerInventorySlot == newPlayerSlot);
        moved.playerInventorySlot = newPlayerSlot;
    }

    public void onMainHandUse(LocalPlayer player) {
        if (player == null) {
            return;
        }
        long currentTick = currentGameTick();
        this.restoreSession.markActivity(currentTick);
        int selectedSlot = me.aleksilassila.litematica.printer.utils.InventoryUtils
                .getSelectedSlot(player.getInventory());
        ItemStack mainHandStack = player.getMainHandItem();
        OrderedStorageEntry statistics = OrderedStorageTracking.findAtSlot(
                this.trackedItems, selectedSlot, mainHandStack);
        if (statistics == null) {
            for (OrderedStorageEntry candidate : this.trackedItems) {
                if (!OrderedStorageTracking.isValid(player, candidate)
                        && OrderedStorageStacks.matches(candidate.itemStack, mainHandStack)) {
                    candidate.playerInventorySlot = selectedSlot;
                    statistics = candidate;
                    break;
                }
            }
        }
        if (statistics != null) {
            statistics.markUsed(currentTick);
        }
    }

    /**
     * Keep enough free inventory slots while printing, then return every
     * tracked stack after the printer has been idle for a short period.
     */
    public boolean maintainOrderlyStorage() {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                //#if MC > 260100
                //$$ || client.gui.screen() != null
                //#else
                || client.screen != null
                //#endif
                || !player.containerMenu.equals(player.inventoryMenu)) {
            return this.restoreSession.hasPending();
        }
        if (this.restoreSession.hasPending()) {
            if (!this.restoreSession.isWaitingForContainer()) {
                openPendingShulker();
            }
            return true;
        }

        OrderedStorageTracking.reconcile(this.trackedItems, player);
        if (this.trackedItems.isEmpty()) {
            this.restoreSession.clearPressureRecovery();
            this.restoreSession.clearActivity();
            return false;
        }

        long currentTick = client.level.getGameTime();
        OrderedStoragePolicy.normalizeActivityTicks(this.restoreSession, this.trackedItems, currentTick);
        int freeSlots = OrderedStoragePolicy.countEmptyInventorySlots(player);
        updatePressureRecovery(freeSlots);
        if (this.restoreSession.isPressureRecoveryActive()) {
            boolean emergency = freeSlots <= EMERGENCY_FREE_SLOTS;
            return scheduleRestore(player, currentTick, emergency, false);
        }
        if (!this.neededItems.get().isEmpty()) {
            return false;
        }

        if (currentTick - this.restoreSession.lastActivityTick() < IDLE_RESTORE_DELAY_TICKS) {
            return false;
        }

        return scheduleRestore(player, currentTick, false, true);
    }

    public boolean tryRestoreForInventoryPressure() {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                || !player.containerMenu.equals(player.inventoryMenu)) {
            return false;
        }
        OrderedStorageTracking.reconcile(this.trackedItems, player);
        if (this.trackedItems.isEmpty()) {
            this.restoreSession.clearPressureRecovery();
            return false;
        }
        long currentTick = client.level.getGameTime();
        OrderedStoragePolicy.normalizeActivityTicks(this.restoreSession, this.trackedItems, currentTick);
        int freeSlots = OrderedStoragePolicy.countEmptyInventorySlots(player);
        updatePressureRecovery(freeSlots);
        return this.restoreSession.isPressureRecoveryActive()
                && scheduleRestore(player, currentTick, freeSlots <= EMERGENCY_FREE_SLOTS, false);
    }

    public boolean hasPendingRestore() {
        return this.restoreSession.hasPending();
    }

    public boolean isWaitingForRestoreContainer() {
        return this.restoreSession.isWaitingForContainer();
    }

    /**
     * Restore to the original inner slot first, then matching partial stacks, then empty slots.
     */
    public void onContainerOpen(int containerId) {
        if (isWaitingForRestoreContainer()) this.automatedContainerId = containerId;
    }

    public void restorePendingItem() {
        if (!this.isWaitingForRestoreContainer() || client.player == null || client.gameMode == null) {
            return;
        }
        LocalPlayer player = client.player;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.equals(player.inventoryMenu) || menu.containerId != automatedContainerId || !menu.getCarried().isEmpty()) {
            return;
        }

        OrderedStorageEntry statistics = this.restoreSession.pending();
        this.restoreSession.stopContainerWait();
        int playerMenuSlot = OrderedStorageStacks.findPlayerInventoryMenuSlot(menu, statistics);
        if (playerMenuSlot < 0) {
            finishPendingRestore(false);
            player.closeContainer();
            return;
        }

        Slot playerSlot = menu.slots.get(playerMenuSlot);
        ItemStack returningStack = playerSlot.getItem();
        List<Integer> destinations = OrderedStorageStacks.buildRestoreDestinations(menu, statistics, returningStack);
        int totalCapacity = 0;
        for (int destination : destinations) {
            totalCapacity += OrderedStorageStacks.availableCapacity(menu.slots.get(destination), returningStack);
            if (totalCapacity >= returningStack.getCount()) {
                break;
            }
        }
        if (!OrderedStorageStacks.matches(statistics.itemStack, returningStack)) {
            finishPendingRestore(false);
            player.closeContainer();
            return;
        }
        if (totalCapacity < returningStack.getCount()) {
            retryPendingRestore();
            player.closeContainer();
            return;
        }

        client.gameMode.handleContainerInput(menu.containerId, playerMenuSlot, 0, ContainerInput.PICKUP, player);
        for (int destination : destinations) {
            if (menu.getCarried().isEmpty()) {
                break;
            }
            client.gameMode.handleContainerInput(menu.containerId, destination, 0, ContainerInput.PICKUP, player);
        }
        boolean restored = menu.getCarried().isEmpty();
        if (!restored) {
            client.gameMode.handleContainerInput(menu.containerId, playerMenuSlot, 0, ContainerInput.PICKUP, player);
        }
        if (restored) {
            finishPendingRestore(true);
        } else {
            retryPendingRestore();
        }
        player.closeContainer();
    }

    public void tick() {
        if (!this.restoreSession.tickContainerTimeout()) {
            return;
        }
        LocalPlayer player = client.player;
        retryPendingRestore();
        ShulkerUtils.recoverFailedOpen();
        ModLoadUtils.closeScreen = 0;
        if (player != null && !player.containerMenu.equals(player.inventoryMenu)
                && player.containerMenu.containerId == automatedContainerId) {
            player.closeContainer();
        }
    }

    public void reset() {
        this.automatedContainerId = -1;
        this.trackedItems.clear();
        this.restoreSession.reset();
    }

    private long currentGameTick() {
        return client.level == null ? 0L : client.level.getGameTime();
    }

    private void markPrinterActivity() {
        this.restoreSession.markActivity(currentGameTick());
    }

    private void updatePressureRecovery(int freeSlots) {
        this.restoreSession.updatePressureRecovery(
                freeSlots,
                PRESSURE_TRIGGER_FREE_SLOTS,
                PRESSURE_TARGET_FREE_SLOTS
        );
    }

    private boolean scheduleRestore(
            LocalPlayer player,
            long currentTick,
            boolean allowRecentlyUsed,
            boolean allowCurrentMainHand
    ) {
        if (this.restoreSession.hasPending()) {
            if (!this.restoreSession.isWaitingForContainer()) {
                openPendingShulker();
            }
            return true;
        }
        OrderedStorageEntry selected = OrderedStoragePolicy.selectRestoreCandidate(
                player,
                this.trackedItems,
                this.neededItems.get(),
                currentTick,
                RECENT_USE_PROTECTION_TICKS,
                allowRecentlyUsed,
                allowCurrentMainHand
        );
        if (selected == null) {
            return false;
        }
        this.restoreSession.schedule(selected);
        openPendingShulker();
        return this.restoreSession.hasPending();
    }

    private void clearPressureRecoveryIfSatisfied() {
        LocalPlayer player = client.player;
        if (player != null
                && OrderedStoragePolicy.countEmptyInventorySlots(player) >= PRESSURE_TARGET_FREE_SLOTS) {
            this.restoreSession.clearPressureRecovery();
        }
    }

    private void openPendingShulker() {
        LocalPlayer player = client.player;
        OrderedStorageEntry pendingRestore = this.restoreSession.pending();
        if (player == null || client.gameMode == null || pendingRestore == null
                || this.restoreSession.isWaitingForContainer()
                || ModLoadUtils.closeScreen > 0
                || !player.containerMenu.equals(player.inventoryMenu)) {
            return;
        }
        OrderedStorageTracking.reconcile(this.trackedItems, player);
        if (!this.trackedItems.contains(pendingRestore)) {
            clearPendingRestore();
            return;
        }

        int shulkerMenuSlot = OrderedStorageStacks.findBestShulkerMenuSlot(player, pendingRestore);
        if (shulkerMenuSlot < 0) {
            finishPendingRestore(false);
            return;
        }
        pendingRestore.shulkerInventoryMenuSlot = shulkerMenuSlot;
        ItemStack shulkerStack = player.inventoryMenu.slots.get(shulkerMenuSlot).getItem();
        this.containerSession.begin(shulkerStack,
                me.aleksilassila.litematica.printer.config.Configs.Placement.QUICK_SHULKER_MODE.getOptionListValue()
                        == me.aleksilassila.litematica.printer.enums.QuickShulkerModeType.CLICK_SLOT);
        if (!ShulkerUtils.openShulker(shulkerStack, shulkerMenuSlot)) {
            this.containerSession.reset();
            retryPendingRestore();
            return;
        }
        ModLoadUtils.closeScreen++;
        this.restoreSession.beginContainerWait(RESTORE_TIMEOUT_TICKS);
    }

    private void finishPendingRestore(boolean success) {
        OrderedStorageEntry completed = this.restoreSession.pending();
        clearPendingRestore();
        if (completed != null) {
            this.trackedItems.remove(completed);
        }
        if (success) {
            clearPressureRecoveryIfSatisfied();
        }
        if (!success) {
            MessageUtils.setOverlayMessage(I18n.INVENTORY_RESTORE_FAILED.getName(), false);
        }
    }

    private void retryPendingRestore() {
        OrderedStorageEntry pendingRestore = this.restoreSession.pending();
        if (pendingRestore != null && pendingRestore.shulkerInventoryMenuSlot >= 0) {
            pendingRestore.attemptedShulkerMenuSlots.add(pendingRestore.shulkerInventoryMenuSlot);
        }
        this.restoreSession.stopContainerWait();
    }

    private void clearPendingRestore() {
        this.restoreSession.clearPending();
    }

}
