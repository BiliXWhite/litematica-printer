package me.aleksilassila.litematica.printer.integration.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickBlockRequestPolicyTest {
    @Test
    void activeProviderConsumesTheInput() {
        assertTrue(PickBlockRequestPolicy.shouldConsume(
                MaterialReservation.State.PENDING
        ));
        assertTrue(PickBlockRequestPolicy.shouldConsume(
                MaterialReservation.State.AVAILABLE
        ));
    }

    @Test
    void unavailableProviderLeavesInputForNativeHooksEvenWhenTakeItOutIsInstalled() {
        assertFalse(PickBlockRequestPolicy.shouldConsume(
                MaterialReservation.State.UNAVAILABLE
        ));
    }
}
