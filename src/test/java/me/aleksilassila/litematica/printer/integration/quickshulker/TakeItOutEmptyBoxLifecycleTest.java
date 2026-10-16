//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import me.aleksilassila.litematica.printer.integration.inventory.*;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TakeItOutEmptyBoxLifecycleTest {
    @BeforeAll static void bootstrap() throws Exception { CancelledSwapLifecycleTest.bootstrap(); }

    private static TakeItOutAdapter pending(CancelledSwapLifecycleTest.Fixture f) throws Exception {
        CancelledSwapLifecycleTest.set(f.player, net.minecraft.world.entity.player.Player.class, "abilities", new Abilities());
        var full = new ItemStack(Items.SHULKER_BOX);
        full.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
        f.player.inventory.setItem(10, full);
        var port = (ActionPort) CancelledSwapLifecycleTest.get(f.adapter, QuickShulkerAdapter.class, "actionBroker");
        var adapter = new TakeItOutAdapter(port);
        CancelledSwapLifecycleTest.set(adapter, TakeItOutAdapter.class, "resourcesAcquired", true);
        CancelledSwapLifecycleTest.set(adapter, TakeItOutAdapter.class, "activeToken", 1L);
        CancelledSwapLifecycleTest.set(adapter, TakeItOutAdapter.class, "requestedItem", Items.SHULKER_BOX);
        f.leased.set(true);
        return adapter;
    }

    private static MaterialRequest request(boolean emptyOnly) {
        return new MaterialRequest(1, List.of(Items.SHULKER_BOX), Items.SHULKER_BOX, 1, MaterialRequest.Source.PRINT, emptyOnly);
    }

    @Test void clearedExternalWaitFlagDoesNotReleaseAnEmptyBoxRequestBeforeArrival() throws Exception {
        try (var f = new CancelledSwapLifecycleTest.Fixture()) {
            var adapter = pending(f);
            for (int poll = 0; poll < 60; poll++) {
                assertEquals(MaterialReservation.State.PENDING, adapter.status(request(true)).state());
                assertTrue(f.leased.get());
            }
            f.player.inventory.setItem(11, new ItemStack(Items.SHULKER_BOX));
            assertEquals(MaterialReservation.State.AVAILABLE, adapter.status(request(true)).state());
            assertFalse(f.leased.get());
        }
    }

    @Test void missingReplyStillTimesOutAndAllowsTheNextProvider() throws Exception {
        try (var f = new CancelledSwapLifecycleTest.Fixture()) {
            var adapter = pending(f);
            var nextCalls = new AtomicInteger();
            var coordinator = new MaterialRequestCoordinator(List.of(new InventoryProvider() {
                public String id() { return "accepted_take_it_out"; }
                public MaterialReservation request(MaterialRequest r) { return new MaterialReservation(r.token(), MaterialReservation.State.PENDING); }
                public MaterialReservation status(MaterialRequest r) { return adapter.status(r); }
                public void reset() { adapter.reset(); }
            }, new InventoryProvider() {
                public String id() { return "next"; }
                public MaterialReservation request(MaterialRequest r) {
                    nextCalls.incrementAndGet();
                    return new MaterialReservation(r.token(), MaterialReservation.State.UNAVAILABLE);
                }
            }));
            coordinator.requestEmptyShulker(Items.SHULKER_BOX);
            for (int tick = 0; tick < 80; tick++) coordinator.tick();
            assertEquals(0, nextCalls.get(), "Do not fall through while the accepted request is still in flight");
            assertTrue(f.leased.get());
            coordinator.tick();
            assertEquals(1, nextCalls.get());
            assertFalse(coordinator.isBusy());
            assertFalse(f.leased.get());
        }
    }

    @Test void resetReleasesWaitingAndOrdinaryRequestsKeepAcceptingFilledBoxes() throws Exception {
        try (var f = new CancelledSwapLifecycleTest.Fixture()) {
            var adapter = pending(f);
            assertEquals(MaterialReservation.State.AVAILABLE, adapter.status(request(false)).state());
            adapter = pending(f);
            adapter.reset();
            assertFalse(f.leased.get());
            assertEquals(0L, CancelledSwapLifecycleTest.get(adapter, TakeItOutAdapter.class, "activeToken"));
        }
    }
}
//#endif
