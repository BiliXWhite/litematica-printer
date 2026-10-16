package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.integration.inventory.InventoryProvider;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialReservation;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;

import java.util.Collection;

/** Public adapter around the Quick Shulker request and ordered-restore controllers. */
public final class QuickShulkerAdapter implements InventoryProvider, RuntimeComponent {
    private static final String LEASE_OWNER = "quick_shulker";
    private final ActionPort actionBroker;
    private final QuickShulkerRequestController requests;
    private final OrderedStorageController orderedStorage;
    private boolean resourcesAcquired;
    private long attemptedToken;
    private boolean externalRequestActive;

    public QuickShulkerAdapter(ActionPort actionBroker) {
        this.actionBroker = actionBroker;
        this.requests = new QuickShulkerRequestController(Minecraft.getInstance());
        this.orderedStorage = this.requests.orderedStorage();
    }

    @Override
    public String id() {
        return "quick_shulker";
    }

    @Override
    public long pendingTimeoutTicks() {
        // Open, clear an occupied hotbar slot, extract, plus cancellation/recovery margin.
        return 160L;
    }

    @Override
    public MaterialReservation request(MaterialRequest request) {
        if (this.requests.isCancelling()) return new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
        if (!Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
            return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
        }
        if (!this.acquireResources()) {
            return new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
        }
        this.requests.requestItems(request.acceptedItems(), request.emptyShulkerOnly());
        this.attemptedToken = request.token();
        // Resolve the queued lookup immediately. A non-empty requested-item set only means that
        // we still need to search the carried shulker boxes; it does not mean that an inventory
        // operation has actually started. Treating that transient set as PENDING makes one absent
        // material hold MAIN_HAND/INVENTORY until the next tick, so printing never reaches the
        // other schematic targets that the player does carry.
        boolean operationStarted = this.requests.switchItem();
        MaterialReservation.State state = operationStarted && this.requests.hasPendingSwitchRequest()
                ? MaterialReservation.State.PENDING : MaterialReservation.State.UNAVAILABLE;
        if (state == MaterialReservation.State.UNAVAILABLE) {
            this.releaseResources();
        }
        return new MaterialReservation(request.token(), state);
    }

    @Override
    public MaterialReservation status(MaterialRequest request) {
        if (!Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
            this.reset();
            return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
        }
        if (this.requests.hasPendingSwitchRequest()) {
            return new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
        }
        Item availableItem = findAvailableItem(request);
        if (availableItem != null) {
            this.releaseResources();
            return MaterialReservation.available(request, availableItem);
        }
        if (!this.resourcesAcquired && this.attemptedToken != request.token()) {
            return this.request(request);
        }
        MaterialReservation.State state = this.requests.hasPendingSwitchRequest()
                ? MaterialReservation.State.PENDING : MaterialReservation.State.UNAVAILABLE;
        if (state == MaterialReservation.State.UNAVAILABLE) {
            this.releaseResources();
        }
        return new MaterialReservation(request.token(), state);
    }

    public boolean switchItem() {
        return this.requests.switchItem();
    }

    public boolean hasPendingRequest() {
        return Configs.Placement.QUICK_SHULKER.getBooleanValue()
                && this.requests.hasPendingSwitchRequest();
    }

    /** Starts a nested-container extraction requested by another material provider. */
    public boolean requestItemsDirect(Collection<Item> items) {
        if (!Configs.Placement.QUICK_SHULKER.getBooleanValue() || items == null || items.isEmpty()) {
            return false;
        }
        if (!Configs.Core.WORK_SWITCH.getBooleanValue()) {
            this.allowExternalRequest();
        }
        if (!this.acquireResources()) {
            return false;
        }
        this.requests.requestItems(items);
        boolean started = this.requests.switchItem();
        this.synchronizeResources();
        return started;
    }

    public boolean isOpenHandler() {
        return Configs.Placement.QUICK_SHULKER.getBooleanValue()
                && this.requests.isOpenHandler();
    }

    public boolean shouldPause() {
        return this.requests.shouldPauseForSwitchRequest();
    }

    public boolean shouldSuppressContainerScreen(int containerId) {
        return this.requests.shouldSuppressContainerScreen(containerId);
    }

    public void onContainerOpen(int containerId, net.minecraft.world.inventory.MenuType<?> type) {
        this.requests.onContainerOpen(containerId, type);
    }

    public void allowExternalRequest() {
        this.externalRequestActive = true;
        this.requests.setExternalRequestAllowed(true);
    }

    public void clearExternalRequestIfIdle(boolean coordinatorBusy) {
        if (this.externalRequestActive && !coordinatorBusy && !this.requests.hasPendingSwitchRequest()) {
            this.externalRequestActive = false;
            this.requests.setExternalRequestAllowed(false);
        }
    }

    @Override
    public void tick() {
        if (this.requests.isCancelling()) {
            if (!this.acquireResources()) return;
            this.requests.tickCancellation();
            this.synchronizeResources();
            return;
        }
        // A manual container must never be affected after the printer has been disabled.
        // Clear the automation session before the screen guard can observe stale state.
        if (!Configs.Placement.QUICK_SHULKER.getBooleanValue()
                || (!Configs.Core.WORK_SWITCH.getBooleanValue() && !this.externalRequestActive)) {
            this.reset();
            return;
        }
        // Maintenance must acquire the same lease before it can send a container click.
        if (!this.acquireResources()) return;
        this.requests.tick();
        this.requests.switchItem();
        this.synchronizeResources();
    }

    public void onSlotUpdate(int containerId, int slot, net.minecraft.world.item.ItemStack stack) {
        this.requests.onSlotUpdate(containerId, slot, stack);
    }

    public void onInventoryContent(int containerId, java.util.List<net.minecraft.world.item.ItemStack> items,
                                   net.minecraft.world.item.ItemStack carried) {
        if (this.requests.isCancelling()) {
            this.requests.finishCancellation(containerId, items.size());
            this.synchronizeResources();
            return;
        }
        if (!Configs.Placement.QUICK_SHULKER.getBooleanValue()) return;
        if (!this.requests.confirmContainer(containerId, items, carried)) return;
        if (this.requests.isOpenHandler()) {
            this.requests.switchInv();
        }
        if (this.orderedStorage.isWaitingForRestoreContainer()) {
            this.orderedStorage.restorePendingItem();
        }
    }

    public void onMainHandUse(LocalPlayer player) {
        this.orderedStorage.onMainHandUse(player);
    }

    @Override
    public void reset() {
        this.orderedStorage.reset();
        this.requests.resetRuntime();
        this.attemptedToken = 0L;
        this.externalRequestActive = false;
        if (this.requests.isCancelling()) this.acquireResources();
        else this.releaseResources();
    }

    @Override
    public void onEpochChanged(RuntimeEvent.EpochChanged event) {
        // The action broker has discarded its leases at this epoch boundary.
        this.resourcesAcquired = false;
        if ("client_scope_changed".equals(event.reason())) this.requests.discardForClientScopeChange();
        me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils.onRuntimeReset(event.reason());
        this.reset();
    }

    private void synchronizeResources() {
        boolean busy = this.requests.hasPendingSwitchRequest()
                || this.requests.isOpenHandler()
                || this.orderedStorage.hasPendingRestore();
        if (busy) {
            this.acquireResources();
        } else {
            this.releaseResources();
        }
    }

    private boolean acquireResources() {
        if (this.resourcesAcquired) return true;
        this.resourcesAcquired = this.actionBroker.tryAcquire(
                LEASE_OWNER,
                java.util.EnumSet.of(ResourceLease.MAIN_HAND, ResourceLease.INVENTORY, ResourceLease.CONTAINER),
                0L
        );
        return this.resourcesAcquired;
    }

    private void releaseResources() {
        if (this.resourcesAcquired) {
            this.actionBroker.releaseOwner(LEASE_OWNER);
            this.resourcesAcquired = false;
        }
    }

    private static Item findAvailableItem(MaterialRequest request) {
        for (Item item : request.acceptedItems()) {
            if (InventoryUtils.playerHasItemInInventory(Minecraft.getInstance().player, item, request::acceptsStack)) {
                return item;
            }
        }
        return null;
    }
}
