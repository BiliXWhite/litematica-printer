package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.inventory.AbstractContainerMenu;

/** Retains cancellation ownership until a full server snapshot replaces unpredicted state. */
final class InventorySyncDrain {
    private AbstractContainerMenu ownedMenu;
    private AbstractContainerMenu requestedMenu;
    private Object connection;

    void begin(AbstractContainerMenu menu, Object connection) {
        if (active() || connection == null) return;
        this.ownedMenu = menu;
        this.connection = connection;
    }

    boolean active() { return this.ownedMenu != null; }
    boolean owns(AbstractContainerMenu menu) { return active() && menu == this.ownedMenu; }

    boolean needsRequest(AbstractContainerMenu current, Object connection) {
        if (this.connection != connection || current == null) {
            reset();
            return false;
        }
        return active() && current != this.requestedMenu;
    }

    void requested(AbstractContainerMenu menu) { this.requestedMenu = menu; }

    boolean accepts(AbstractContainerMenu current, Object connection, int containerId, int size) {
        return active() && this.connection == connection && this.requestedMenu == current
                && current != null && current.containerId == containerId && current.slots.size() == size;
    }

    void reset() {
        this.ownedMenu = null;
        this.requestedMenu = null;
        this.connection = null;
    }
}
