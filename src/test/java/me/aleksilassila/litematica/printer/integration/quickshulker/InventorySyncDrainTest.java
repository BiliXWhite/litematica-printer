package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventorySyncDrainTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }
    static class Menu extends AbstractContainerMenu {
        Menu(int id) { super(null, id); addSlot(new Slot(new SimpleContainer(1), 0, 0, 0)); }
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    }

    @Test void cancellationWaitsPastTimeoutAndRepeatedDisableUntilFullSnapshot() {
        var drain = new InventorySyncDrain();
        var menu = new Menu(7);
        var connection = new Object();
        drain.begin(menu, connection);
        assertTrue(drain.needsRequest(menu, connection));
        drain.requested(menu);
        for (int tick = 0; tick < 300; tick++) {
            drain.begin(menu, connection); // repeated reset while WORK/QUICK_SHULKER is off
            assertFalse(drain.needsRequest(menu, connection));
            assertTrue(drain.active());
        }
        assertFalse(drain.accepts(menu, connection, 8, 1));
        assertFalse(drain.accepts(menu, connection, 7, 0));
        assertTrue(drain.accepts(menu, connection, 7, 1));
        assertTrue(drain.owns(menu));
        drain.reset();
        assertFalse(drain.active());
    }

    @Test void closedMenuResynchronizesCurrentInventoryWithoutClosingManualMenus() {
        var drain = new InventorySyncDrain();
        var original = new Menu(7);
        var inventory = new Menu(0);
        var connection = new Object();
        drain.begin(original, connection);
        drain.requested(original);
        assertFalse(drain.accepts(inventory, connection, 7, 1));
        assertTrue(drain.needsRequest(inventory, connection));
        drain.requested(inventory);
        assertTrue(drain.accepts(inventory, connection, 0, 1));
        assertFalse(drain.owns(inventory));
    }

    @Test void changingConnectionOrResetCannotAcceptReusedContainerIds() {
        var drain = new InventorySyncDrain();
        var old = new Menu(7);
        var connection = new Object();
        drain.begin(old, connection);
        drain.requested(old);
        assertFalse(drain.accepts(new Menu(7), connection, 7, 1));
        assertFalse(drain.accepts(old, new Object(), 7, 1));
        assertFalse(drain.needsRequest(old, new Object()));
        assertFalse(drain.active());
        drain.reset();
        assertFalse(drain.accepts(old, connection, 7, 1));
    }
}
