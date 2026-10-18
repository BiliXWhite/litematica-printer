//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.util.List;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeScope;
import me.aleksilassila.litematica.printer.integration.inventory.ChestTrackerAdapter;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequestCoordinator;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.action.ActionBroker;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static me.aleksilassila.litematica.printer.integration.quickshulker.CancelledSwapLifecycleTest.set;
import static me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerPacketContractTest.allocate;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real menu ownership and the inventory components' shared epoch-reset order. */
class ChestTrackerOwnershipTest {
    @BeforeAll static void bootstrap() throws Exception {
        CancelledSwapLifecycleTest.bootstrap();
    }

    static class Fixture extends CancelledSwapLifecycleTest.Fixture {
        final PrinterRuntime runtime = RuntimeAccess.get();
        final ActionBroker broker = new ActionBroker(runtime, new ActionManager());
        final ChestTrackerAdapter chest = new ChestTrackerAdapter(broker);

        Fixture() throws Exception {
            set(runtime, PrinterRuntime.class, "epoch", RuntimeEpoch.INITIAL);
            set(runtime, PrinterRuntime.class, "actionBroker", broker);
            set(adapter, QuickShulkerAdapter.class, "actionBroker", broker);
            client.level = allocate(ClientLevel.class);
        }

        void receiveChestOpen() throws Exception {
            // The remote-open request has been sent; the actual callback must own the new menu.
            set(chest, ChestTrackerAdapter.class, "activeRequest",
                    MaterialRequest.single(1L, Items.STONE, 1, MaterialRequest.Source.PRINT));
            var phaseType = ChestTrackerAdapter.class.getDeclaredField("phase").getType();
            @SuppressWarnings({"unchecked", "rawtypes"})
            var waiting = Enum.valueOf((Class) phaseType, "WAITING_CONTENT");
            set(chest, ChestTrackerAdapter.class, "phase", waiting);
            set(chest, ChestTrackerAdapter.class, "suppressContainerScreen", true);
            chest.onContainerOpen(menu.containerId);
        }
    }

    @Test void idleResetPreservesAManuallyOpenedMenu() throws Exception {
        try (var f = new Fixture()) {
            var manual = new InventorySyncDrainTest.Menu(12);
            f.player.containerMenu = manual;

            f.chest.reset();

            assertSame(manual, f.player.containerMenu);
            assertFalse(f.player.closed, "An idle optional provider must not close a manual menu");
        }
    }

    @Test void resetStillClosesTheMenuOpenedForItsOwnRequest() throws Exception {
        try (var f = new Fixture()) {
            f.receiveChestOpen();

            f.chest.reset();

            assertTrue(f.player.closed);
            assertSame(f.player.inventoryMenu, f.player.containerMenu);
            assertFalse(f.chest.shouldSuppressContainerScreen());
        }
    }

    @Test void anOldMenuIdDoesNotAuthorizeClosingItsReplacement() throws Exception {
        try (var f = new Fixture()) {
            f.receiveChestOpen();
            var replacement = new InventorySyncDrainTest.Menu(f.menu.containerId);
            f.player.containerMenu = replacement;

            f.chest.reset();

            assertSame(replacement, f.player.containerMenu);
            assertFalse(f.player.closed, "Container IDs may be reused for another menu");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"connection", "level"})
    void aChangedClientScopeInvalidatesEvenTheOldMenuReference(String changed) throws Exception {
        try (var f = new Fixture()) {
            f.receiveChestOpen();
            if (changed.equals("connection")) {
                set(f.player, LocalPlayer.class, "connection", allocate(CancelledSwapLifecycleTest.Connection.class));
            } else {
                f.client.level = allocate(ClientLevel.class);
            }

            f.chest.onEpochChanged(new RuntimeEvent.EpochChanged(
                    RuntimeEpoch.INITIAL, RuntimeEpoch.INITIAL.next(), "client_scope_changed"));

            assertSame(f.menu, f.player.containerMenu);
            assertFalse(f.player.closed, "Old ownership cannot cross a client scope change");
        }
    }

    @Test void changingServerDoesNotCloseTheNewServersMenuWithTheSameId() throws Exception {
        try (var f = new Fixture()) {
            f.receiveChestOpen();
            set(f.player, LocalPlayer.class, "connection", allocate(CancelledSwapLifecycleTest.Connection.class));
            var replacement = new InventorySyncDrainTest.Menu(f.menu.containerId);
            f.player.containerMenu = replacement;

            f.chest.onEpochChanged(new RuntimeEvent.EpochChanged(
                    RuntimeEpoch.INITIAL, RuntimeEpoch.INITIAL.next(), "client_scope_changed"));

            assertSame(replacement, f.player.containerMenu);
            assertFalse(f.player.closed);
        }
    }

    @Test void sharedScopeResetWaitsForTheQuickShulkerReplyBeforeClosingItsMenu() throws Exception {
        try (var f = new Fixture()) {
            var scope = new RuntimeScope();
            // Same relative registration order as PrinterRuntime, including the coordinator's
            // second provider-reset pass; all these lifecycle components are real instances.
            scope.register(f.broker);
            scope.register(f.adapter);
            scope.register(f.chest);
            scope.register(new MaterialRequestCoordinator(List.of(f.adapter, f.chest)));
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
            var nextEpoch = RuntimeEpoch.INITIAL.next();
            set(f.runtime, PrinterRuntime.class, "epoch", nextEpoch);

            scope.changeEpoch(new RuntimeEvent.EpochChanged(RuntimeEpoch.INITIAL, nextEpoch, "work_switch_off"));

            assertSame(f.menu, f.player.containerMenu);
            assertFalse(f.player.closed, "An idle ChestTracker must not close QuickShulker's draining menu");
            for (int tick = 0; tick < 300; tick++) f.adapter.tick();
            assertSame(f.menu, f.player.containerMenu);
            assertTrue(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertTrue(f.broker.isResourceHeld(ResourceLease.CONTAINER));
            assertEquals(2, f.connection.packets.size(), "Only the original swap and its resync may be sent");

            var items = List.of(ItemStack.EMPTY, new ItemStack(Items.STONE, 64));
            f.menu.initializeContents(9, items, ItemStack.EMPTY);
            f.adapter.onInventoryContent(f.menu.containerId, items, ItemStack.EMPTY);

            assertTrue(f.player.closed);
            assertSame(f.player.inventoryMenu, f.player.containerMenu);
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertFalse(f.broker.isResourceHeld(ResourceLease.CONTAINER));
            assertEquals(64, f.player.inventory.getItem(2).getCount());
        }
    }
}
//#endif
