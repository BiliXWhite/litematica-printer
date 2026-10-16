package me.aleksilassila.litematica.printer.handler.handlers;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrenchFillStateTest {
    @Test
    void delayedWorldConfirmationKeepsTheTargetInFlight() {
        TrenchFillState state = new TrenchFillState();
        BlockPos pos = new BlockPos(1, 2, 3);
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 10L, ignored -> true);
        state.markInFlight(pos, 10L, true);
        for (long tick = 11L; tick <= 50L; tick++) {
            state.beginTick();
            state.updateTargets(List.of(), Set.of(pos), Set.of(), tick, ignored -> true);
            assertTrue(state.hasWork());
            assertFalse(state.canAttempt(pos, tick), "Do not resend while waiting for the world update");
        }
        state.onBlockUpdated(pos, false);
        assertFalse(state.hasWork());
    }

    @Test
    void rejectedAndTimedOutPlacementsBecomeRetryable() {
        TrenchFillState state = new TrenchFillState();
        BlockPos pos = new BlockPos(1, 2, 3);
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 10L, ignored -> true);
        state.markInFlight(pos, 10L, true);
        state.onBlockUpdated(pos, true);
        assertTrue(state.hasFillTarget(pos));
        assertTrue(state.canAttempt(pos, 11L));
        state.markInFlight(pos, 11L, false);
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 52L, ignored -> true);
        assertTrue(state.canAttempt(pos, 52L));
    }

    @Test
    void resetDiscardsOldWorldRepliesAndAllowsTheNewWorldTarget() {
        TrenchFillState state = new TrenchFillState();
        BlockPos pos = new BlockPos(1, 2, 3);
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 500L, ignored -> true);
        state.markInFlight(pos, 500L, true);
        state.clear();
        state.onBlockUpdated(pos, false);
        assertFalse(state.hasWork());
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 1L, ignored -> true);
        assertTrue(state.canAttempt(pos, 1L));
        assertTrue(state.canSend(1L, 20, 1, true));
    }

    @Test
    void retryAndWorldUpdateControlFillReadiness() {
        TrenchFillState state = new TrenchFillState();
        BlockPos pos = new BlockPos(1, 2, 3);
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 10L, ignored -> true);

        state.defer(pos, 15L);
        assertFalse(state.canAttempt(pos, 14L));
        assertTrue(state.canAttempt(pos, 15L));

        state.markInFlight(pos, 15L, true);
        assertFalse(state.canAttempt(pos, 16L));
        state.onBlockUpdated(pos, false);
        assertFalse(state.hasWork());
    }

    @Test
    void resetDropsStaleTargetsAndRateHistory() {
        TrenchFillState state = new TrenchFillState();
        BlockPos pos = new BlockPos(1, 2, 3);
        state.updateTargets(List.of(), Set.of(pos), Set.of(), 10L, ignored -> true);
        state.markInFlight(pos, 10L, true);
        assertEquals(1, state.sentThisTick());

        state.clear();

        assertFalse(state.hasWork());
        assertEquals(0, state.sentThisTick());
        assertEquals(Long.MIN_VALUE, state.lastSentTick());
    }

    @Test
    void fillBatchRespectsPerTickLimitWithoutSerializingSameTick() {
        TrenchFillState state = new TrenchFillState();

        assertTrue(state.canSend(10L, 2, 3, true));
        state.markInFlight(new BlockPos(1, 2, 3), 10L, true);
        assertTrue(state.canSend(10L, 2, 3, false));
        state.markInFlight(new BlockPos(2, 2, 3), 10L, true);
        state.markInFlight(new BlockPos(3, 2, 3), 10L, true);
        assertFalse(state.canSend(10L, 2, 3, false));

        state.beginTick();
        assertFalse(state.canSend(11L, 2, 3, true));
        assertTrue(state.canSend(12L, 2, 3, true));
    }

    @Test
    void distantTrenchTargetsDoNotEnterTheActionBatch() {
        TrenchFillState state = new TrenchFillState();
        BlockPos near = new BlockPos(1, 2, 3);
        BlockPos far = new BlockPos(100, 2, 3);
        state.updateTargets(List.of(), Set.of(near, far), Set.of(far), 10L, ignored -> true);

        assertEquals(List.of(near), state.readyFillTargets(10L, ignored -> true,
                pos -> pos.equals(near)));
        assertEquals(List.of(), state.readyWaterloggedTargets(pos -> pos.equals(near)));
    }
}
