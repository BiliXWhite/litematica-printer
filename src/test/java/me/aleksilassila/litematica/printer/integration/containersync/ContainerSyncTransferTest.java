package me.aleksilassila.litematica.printer.integration.containersync;

import me.aleksilassila.litematica.printer.core.action.*;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static me.aleksilassila.litematica.printer.integration.containersync.ContainerSyncPlanTest.*;

class ContainerSyncTransferTest {
    @BeforeAll static void setup() { bootstrap(); }
    static class Port implements ContainerSyncTransfer.Port {
        Object context = new Object();
        final ActionCoordinator locks = new ActionCoordinator();
        final List<Integer> clicks = new ArrayList<>();
        int refreshes;
        boolean unchanged = true;
        public Object context() { return context; }
        public boolean acquire() { return acquire("sync"); }
        boolean acquire(String owner) {
            return locks.tryAdmit(new ActionRequest(owner, RuntimeEpoch.INITIAL,
                    EnumSet.of(ResourceLease.INVENTORY, ResourceLease.CONTAINER), 0), 0).isPresent();
        }
        public void release() { locks.releaseOwner("sync"); }
        public void click(int slot, int button) { clicks.add(slot); }
        public void refresh() { refreshes++; }
        public boolean unchanged(List<ItemStack> items, ItemStack cursor) { return unchanged; }
    }
    static ContainerSyncPlan plan() {
        return ContainerSyncPlan.create(List.of(stone(1)), List.of(ItemStack.EMPTY, stone(4)), RULES);
    }
    static void confirm(ContainerSyncTransfer transfer, Port port, ContainerSyncPlan.Step step) {
        transfer.observe(port.context, step.items(), step.cursor());
    }

    @Test void highLatencyNeverResendsPickupOrReleasesInventory() {
        var port = new Port(); var transfer = new ContainerSyncTransfer(port, plan());
        transfer.tick(0, true);
        for (int i = 1; i <= 100; i++) transfer.tick(i * 1000L, true);
        assertEquals(List.of(1), port.clicks);
        assertEquals(0, port.refreshes, "A forced-full click already requests its reply; retries must not leave duplicate snapshots in flight");
        assertFalse(port.acquire("printer"));
        confirm(transfer, port, plan().steps().get(0));
        transfer.tick(101000, true);
        assertEquals(List.of(1, 0), port.clicks);
    }

    @Test void disablingDuringPickupReturnsCursorWithoutStartingInsertion() {
        var port = new Port(); var plan = plan(); var transfer = new ContainerSyncTransfer(port, plan);
        transfer.tick(0, true);
        transfer.tick(1, false);
        assertFalse(port.acquire("take-it-out"));
        confirm(transfer, port, plan.steps().get(0)); transfer.tick(2, false);
        assertFalse(transfer.finished());
        transfer.observe(port.context, List.of(ItemStack.EMPTY, stone(4)), ItemStack.EMPTY);
        transfer.tick(3, false);
        assertTrue(transfer.finished());
        assertTrue(port.acquire("take-it-out"));
        assertEquals(List.of(1, 1), port.clicks);
    }

    @Test void disabledHighLatencyRecoveryKeepsLeaseUntilTheReturnIsConfirmed() {
        var port = new Port(); var plan = plan(); var transfer = new ContainerSyncTransfer(port, plan);
        transfer.tick(0, true);
        for (int second = 1; second <= 100; second++) transfer.tick(second * 1000L, false);
        assertEquals(List.of(1), port.clicks);
        assertFalse(port.acquire("quick-shulker"));
        confirm(transfer, port, plan.steps().get(0));
        transfer.tick(101000, false);
        for (int second = 102; second <= 200; second++) transfer.tick(second * 1000L, false);
        assertEquals(List.of(1, 1), port.clicks);
        assertEquals(0, port.refreshes);
        assertFalse(port.acquire("take-it-out"));
        transfer.observe(port.context, List.of(ItemStack.EMPTY, stone(4)), ItemStack.EMPTY);
        assertTrue(transfer.finished());
        assertFalse(transfer.completed());
        assertTrue(port.acquire("printer"));
    }

    @Test void changingServerDiscardsLateRepliesAndNeverSendsCleanupToNewMenu() {
        var port = new Port(); var plan = plan(); var transfer = new ContainerSyncTransfer(port, plan);
        Object old = port.context;
        transfer.tick(0, true);
        port.context = new Object();
        transfer.observe(old, plan.steps().get(0).items(), plan.steps().get(0).cursor());
        transfer.tick(1, true);
        assertTrue(transfer.finished());
        assertEquals(List.of(1), port.clicks);
        assertTrue(port.acquire("printer"));
    }

    @Test void conflictingInventoryOwnerPreventsAnyClick() {
        var port = new Port(); assertTrue(port.acquire("quick-shulker"));
        var transfer = new ContainerSyncTransfer(port, plan());
        transfer.tick(0, true);
        assertTrue(port.clicks.isEmpty());
        port.locks.releaseOwner("quick-shulker");
        transfer.tick(1, true);
        assertEquals(List.of(1), port.clicks);
    }

    @Test void rejectionWithEmptyCursorPausesInsteadOfRepeatingPickup() {
        var port = new Port(); var transfer = new ContainerSyncTransfer(port, plan());
        transfer.tick(0, true);
        transfer.observe(port.context, List.of(ItemStack.EMPTY, stone(4)), ItemStack.EMPTY);
        transfer.tick(6000, true);
        assertTrue(transfer.finished());
        assertNotNull(transfer.problem());
        assertEquals(List.of(1), port.clicks);
    }

    @Test void epochLeaseResetIsReacquiredWhileCancelledClickIsInFlight() {
        var port = new Port(); var transfer = new ContainerSyncTransfer(port, plan());
        transfer.tick(0, true);
        port.locks.reset();
        transfer.cancel();
        transfer.tick(100, false);
        assertFalse(port.acquire("printer"));
        assertEquals(List.of(1), port.clicks);
    }

    @Test void serverCorrectionReturnsOwnedCursorBeforeReleasingInventory() {
        var port = new Port(); var plan = plan(); var transfer = new ContainerSyncTransfer(port, plan);
        transfer.tick(0, true);
        confirm(transfer, port, plan.steps().get(0));
        transfer.tick(1, true);
        // Server rejected the insertion: the four stones are still on the cursor.
        transfer.observe(port.context, List.of(ItemStack.EMPTY, ItemStack.EMPTY), stone(4));
        transfer.tick(6000, true);
        assertEquals(List.of(1, 0, 1), port.clicks);
        assertFalse(port.acquire("printer"));
        transfer.observe(port.context, List.of(ItemStack.EMPTY, stone(4)), ItemStack.EMPTY);
        assertTrue(transfer.finished());
        assertTrue(port.acquire("printer"));
        assertNotNull(transfer.problem());
    }

    @Test void foreignCursorRequiresManualRecoveryAndDoesNotGetPutIntoOwnedSource() {
        var port = new Port(); var transfer = new ContainerSyncTransfer(port, plan());
        transfer.tick(0, true);
        transfer.observe(port.context, List.of(ItemStack.EMPTY, ItemStack.EMPTY), new ItemStack(net.minecraft.world.item.Items.DIRT));
        transfer.tick(6000, false);
        assertTrue(transfer.needsAttention());
        assertFalse(transfer.finished());
        assertEquals(List.of(1), port.clicks);
        // Even if manual recovery temporarily recreates the planned cursor, automation must stay stopped.
        confirm(transfer, port, plan().steps().get(0));
        transfer.tick(6001, false);
        assertEquals(List.of(1), port.clicks);
        transfer.observe(port.context, List.of(ItemStack.EMPTY, stone(4)), ItemStack.EMPTY);
        transfer.tick(12000, false);
        assertTrue(transfer.finished());
    }

    @Test void cancellationDoesNotBeginTheNextIndependentMove() {
        var port = new Port();
        var plan = ContainerSyncPlan.create(List.of(stone(1), stone(1)),
                List.of(ItemStack.EMPTY, ItemStack.EMPTY, stone(4)), RULES);
        var transfer = new ContainerSyncTransfer(port, plan);
        transfer.tick(0, true); transfer.cancel();
        confirm(transfer, port, plan.steps().get(0)); transfer.tick(1, false);
        transfer.observe(port.context, List.of(ItemStack.EMPTY, ItemStack.EMPTY, stone(4)), ItemStack.EMPTY);
        transfer.tick(2, false);
        assertTrue(transfer.finished());
        assertFalse(transfer.completed());
        assertEquals(List.of(2, 2), port.clicks);
    }

    @Test void disablingWithInsertionInFlightWaitsThenReturnsOnlyRemainingCursor() {
        var port = new Port(); var plan = plan(); var transfer = new ContainerSyncTransfer(port, plan);
        transfer.tick(0, true); confirm(transfer, port, plan.steps().get(0)); transfer.tick(1, true);
        transfer.tick(2, false);
        assertEquals(List.of(1, 0), port.clicks);
        confirm(transfer, port, plan.steps().get(1)); transfer.tick(3, false);
        assertFalse(port.acquire("printer"));
        transfer.observe(port.context, List.of(stone(1), stone(3)), ItemStack.EMPTY);
        assertTrue(transfer.finished());
        assertEquals(List.of(1, 0, 1), port.clicks);
        assertTrue(port.acquire("printer"));
    }

    @Test void interveningSlotUpdateStopsNextClickAndRequestsAuthoritativeState() {
        var port = new Port(); var transfer = new ContainerSyncTransfer(port, plan());
        transfer.tick(0, true);
        confirm(transfer, port, plan().steps().get(0));
        port.unchanged = false;
        transfer.tick(1, true);
        assertEquals(List.of(1), port.clicks);
        assertEquals(1, port.refreshes);
        assertFalse(port.acquire("printer"));
    }

    @Test void oldFullSnapshotsCannotAcknowledgeNextClickEvenAcrossRevisionWrap() {
        assertFalse(ContainerSyncTransfer.newerRevision(12, 12));
        assertFalse(ContainerSyncTransfer.newerRevision(11, 12));
        assertTrue(ContainerSyncTransfer.newerRevision(13, 12));
        assertTrue(ContainerSyncTransfer.newerRevision(0, 32767));
        assertFalse(ContainerSyncTransfer.newerRevision(32767, 0));
    }
}
