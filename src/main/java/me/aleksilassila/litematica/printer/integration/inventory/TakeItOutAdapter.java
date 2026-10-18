package me.aleksilassila.litematica.printer.integration.inventory;

import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.mods.TakeItOutUtils;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.EnumSet;

/** Reflection-backed Take It Out capability isolated from feature code. */
public final class TakeItOutAdapter implements InventoryProvider {
    private static final String LEASE_OWNER = "take_it_out";
    private final ActionPort actionBroker;
    private final TakeItOutPort takeItOut;
    private boolean resourcesAcquired;
    private long activeToken;
    private Item requestedItem;
    private int acquireRetries;
    private boolean waitingForExternal;
    private ItemStack ownedPending = ItemStack.EMPTY;
    private Object requestConnection;
    private Object requestLevel;

    public TakeItOutAdapter(ActionPort actionBroker) {
        this(actionBroker, new TakeItOutPort() {
            @Override public ItemStack awaitingStack() { return TakeItOutUtils.getAwaitingStack(); }
            @Override public boolean tryRequestItem(Item item, boolean emptyShulkerOnly) {
                return TakeItOutUtils.tryRequestItem(item, emptyShulkerOnly);
            }
            @Override public void resetPending() { TakeItOutUtils.resetPending(); }
        });
    }

    public TakeItOutAdapter(ActionPort actionBroker, TakeItOutPort takeItOut) {
        this.actionBroker = actionBroker;
        this.takeItOut = takeItOut;
    }

    @Override
    public String id() {
        return "take_it_out";
    }

    @Override
    public MaterialReservation request(MaterialRequest request) {
        if (!this.acquireResources()) {
            return pending(request);
        }
        if (!this.takeItOut.awaitingStack().isEmpty()) {
            this.waitingForExternal = true;
            return pending(request);
        }
        this.waitingForExternal = false;
        Item availableItem = findAvailableItem(request);
        if (availableItem != null) {
            selectCompletedPickBlockItem(request, availableItem);
            this.releaseResources();
            return MaterialReservation.available(request, availableItem);
        }
        for (Item item : request.acceptedItems()) {
            if (this.takeItOut.tryRequestItem(item, request.emptyShulkerOnly())) {
                this.activeToken = request.token();
                this.requestedItem = item;
                this.ownedPending = this.takeItOut.awaitingStack();
                this.requestConnection = Minecraft.getInstance().getConnection();
                this.requestLevel = Minecraft.getInstance().level;
                return pending(request);
            }
        }
        this.releaseResources();
        return unavailable(request);
    }

    @Override
    public MaterialReservation status(MaterialRequest request) {
        if (this.waitingForExternal) {
            if (!this.takeItOut.awaitingStack().isEmpty()) {
                return pending(request);
            }
            this.waitingForExternal = false;
            return this.request(request);
        }
        Item availableItem = findAvailableItem(request);
        if (availableItem != null) {
            selectCompletedPickBlockItem(request, availableItem);
            this.releaseResources();
            return MaterialReservation.available(request, availableItem);
        }
        if (!this.resourcesAcquired) {
            if (this.acquireRetries++ >= 10) {
                this.acquireRetries = 0;
                this.releaseResources();
                return unavailable(request);
            }
            return this.request(request);
        }
        if (this.activeToken == request.token()
                && this.requestedItem != null
                // Take It Out clears awaitingStack for any same-type local stack, including a
                // filled box. Only actual empty-box availability or the coordinator's bounded
                // timeout can finish this stricter request after it has been accepted.
                && (request.emptyShulkerOnly() || this.takeItOut.isAwaitingItem(this.requestedItem))) {
            return pending(request);
        }
        this.releaseResources();
        return unavailable(request);
    }

    @Override
    public boolean isWaitingToStart() {
        return this.waitingForExternal;
    }

    @Override
    public void reset() {
        Minecraft client = Minecraft.getInstance();
        if (!this.ownedPending.isEmpty()
                && this.takeItOut.awaitingStack() == this.ownedPending
                && client.getConnection() == this.requestConnection
                && client.level == this.requestLevel) {
            this.takeItOut.resetPending();
        }
        this.releaseResources();
    }

    private static MaterialReservation pending(MaterialRequest request) {
        return new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
    }

    private static MaterialReservation unavailable(MaterialRequest request) {
        return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
    }

    private boolean acquireResources() {
        if (this.resourcesAcquired) {
            return true;
        }
        this.resourcesAcquired = this.actionBroker.tryAcquire(
                LEASE_OWNER,
                EnumSet.of(ResourceLease.MAIN_HAND, ResourceLease.INVENTORY, ResourceLease.CONTAINER),
                0L
        );
        return this.resourcesAcquired;
    }

    private void releaseResources() {
        if (this.resourcesAcquired) {
            this.actionBroker.releaseOwner(LEASE_OWNER);
        }
        this.resourcesAcquired = false;
        this.activeToken = 0L;
        this.requestedItem = null;
        this.acquireRetries = 0;
        this.waitingForExternal = false;
        this.ownedPending = ItemStack.EMPTY;
        this.requestConnection = null;
        this.requestLevel = null;
    }

    private static Item findAvailableItem(MaterialRequest request) {
        for (Item item : request.acceptedItems()) {
            if (InventoryUtils.playerHasItemInInventory(Minecraft.getInstance().player, item, request::acceptsStack)) {
                return item;
            }
        }
        return null;
    }

    private static void selectCompletedPickBlockItem(MaterialRequest request, Item item) {
        Minecraft client = Minecraft.getInstance();
        if (request.source() == MaterialRequest.Source.PICK_BLOCK
                && client.player != null
                && client.player.containerMenu == client.player.inventoryMenu) {
            InventoryUtils.setPickedItemToHand(new ItemStack(item), client);
        }
    }
}
