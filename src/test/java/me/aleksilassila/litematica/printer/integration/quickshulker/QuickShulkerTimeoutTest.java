package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.integration.inventory.*;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QuickShulkerTimeoutTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }

    @Test void coordinatorAllowsOpenClearHotbarAndExtractRoundTrips() {
        var quick = new QuickShulkerAdapter(null);
        class DelayedReplies implements InventoryProvider {
            boolean completed;
            public String id() { return "quick_shulker"; }
            public MaterialReservation request(MaterialRequest request) { return status(request); }
            public MaterialReservation status(MaterialRequest request) {
                return completed ? MaterialReservation.available(request, Items.STONE)
                        : new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
            }
            public long pendingTimeoutTicks() { return quick.pendingTimeoutTicks(); }
        }
        var replies = new DelayedReplies();
        var coordinator = new MaterialRequestCoordinator(List.of(replies));
        coordinator.request(Items.STONE, MaterialRequest.Source.PRINT);
        // Three sequential operations can each consume nearly forty ticks on a slow server.
        for (int tick = 0; tick < 100; tick++) coordinator.tick();
        assertTrue(coordinator.isBusy());
        replies.completed = true;
        coordinator.tick();
        assertFalse(coordinator.isBusy());
    }
}
