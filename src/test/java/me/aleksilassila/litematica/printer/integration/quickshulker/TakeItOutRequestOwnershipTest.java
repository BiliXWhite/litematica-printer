//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.util.ArrayList;
import java.util.List;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeScope;
import me.aleksilassila.litematica.printer.integration.inventory.*;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.action.ActionBroker;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static me.aleksilassila.litematica.printer.integration.quickshulker.CancelledSwapLifecycleTest.set;
import static me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerPacketContractTest.allocate;
import static org.junit.jupiter.api.Assertions.*;

class TakeItOutRequestOwnershipTest {
    @BeforeAll static void bootstrap() throws Exception { CancelledSwapLifecycleTest.bootstrap(); }

    /** Only the optional mod boundary is replaced; its legacy busy=true result is intentional. */
    static class ExternalApi implements TakeItOutPort {
        ItemStack pending = ItemStack.EMPTY;
        final List<Item> sent = new ArrayList<>();
        final List<Boolean> emptyOnly = new ArrayList<>();
        @Override public ItemStack awaitingStack() { return pending; }
        @Override public boolean tryRequestItem(Item item, boolean emptyShulkerOnly) {
            if (!pending.isEmpty()) return true;
            sent.add(item);
            emptyOnly.add(emptyShulkerOnly);
            pending = new ItemStack(item);
            return true;
        }
        @Override public void resetPending() { pending = ItemStack.EMPTY; }
    }

    static class NextProvider implements InventoryProvider {
        int calls;
        @Override public String id() { return "fallback"; }
        @Override public MaterialReservation request(MaterialRequest request) {
            calls++;
            return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
        }
    }

    static class Fixture extends CancelledSwapLifecycleTest.Fixture {
        final PrinterRuntime runtime = RuntimeAccess.get();
        final ActionBroker broker = new ActionBroker(runtime, new ActionManager());
        final ExternalApi api = new ExternalApi();
        final TakeItOutAdapter takeItOut = new TakeItOutAdapter(broker, api);
        final NextProvider next = new NextProvider();
        final MaterialRequestCoordinator coordinator = new MaterialRequestCoordinator(List.of(takeItOut, next));

        Fixture() throws Exception {
            set(runtime, PrinterRuntime.class, "epoch", RuntimeEpoch.INITIAL);
            set(runtime, PrinterRuntime.class, "actionBroker", broker);
            set(player, net.minecraft.world.entity.player.Player.class, "abilities", new Abilities());
            player.containerMenu = player.inventoryMenu;
            client.level = allocate(ClientLevel.class);
        }

        void tick(int count) { for (int tick = 0; tick < count; tick++) coordinator.tick(); }

        void resetScope(String reason) throws Exception {
            var scope = new RuntimeScope();
            scope.register(broker);
            scope.register(coordinator);
            var nextEpoch = runtime.epoch().next();
            var event = new RuntimeEvent.EpochChanged(runtime.epoch(), nextEpoch, reason);
            set(runtime, PrinterRuntime.class, "epoch", nextEpoch);
            scope.changeEpoch(event);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void anExternalRequestMustFinishBeforeThisTokenCanSend(boolean sameItem) throws Exception {
        try (var f = new Fixture()) {
            var external = new ItemStack(sameItem ? Items.STONE : Items.DIRT);
            f.api.pending = external;
            assertEquals(MaterialReservation.State.PENDING,
                    f.coordinator.request(Items.STONE, MaterialRequest.Source.PRINT).state());

            f.tick(300);

            assertTrue(f.coordinator.isBusy(), "Waiting for an external request is not this token's response timeout");
            assertSame(external, f.api.pending);
            assertTrue(f.api.sent.isEmpty(), "The legacy busy=true result does not mean a new request was sent");
            assertEquals(0, f.next.calls);
            assertTrue(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertTrue(f.broker.isResourceHeld(ResourceLease.CONTAINER));

            f.api.pending = ItemStack.EMPTY;
            f.coordinator.tick();

            assertEquals(List.of(Items.STONE), f.api.sent);
            assertTrue(f.coordinator.isBusy());
            f.player.inventory.setItem(10, new ItemStack(Items.STONE, 16));
            f.api.pending = ItemStack.EMPTY;
            f.coordinator.tick();
            assertFalse(f.coordinator.isBusy());
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertEquals(0, f.next.calls);
        }
    }

    @Test void theFullResponseTimeoutStartsOnlyAfterTheNewRequestWasSent() throws Exception {
        try (var f = new Fixture()) {
            f.api.pending = new ItemStack(Items.DIRT);
            f.coordinator.request(Items.STONE, MaterialRequest.Source.PRINT);
            f.tick(100);
            f.api.pending = ItemStack.EMPTY;
            f.coordinator.tick();
            assertEquals(List.of(Items.STONE), f.api.sent);

            f.tick(80);

            assertTrue(f.coordinator.isBusy());
            assertEquals(0, f.next.calls);
            assertTrue(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            f.coordinator.tick();
            assertFalse(f.coordinator.isBusy());
            assertEquals(1, f.next.calls);
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertTrue(f.api.pending.isEmpty());
            assertEquals(List.of(Items.STONE), f.api.sent, "Timeout must not send the request twice");
        }
    }

    @Test void disablingWorkDoesNotCancelAnExternalRequestItNeverOwned() throws Exception {
        try (var f = new Fixture()) {
            var external = new ItemStack(Items.DIRT);
            f.api.pending = external;
            f.coordinator.request(Items.STONE, MaterialRequest.Source.PRINT);
            Configs.Core.WORK_SWITCH.setBooleanValue(false);

            f.resetScope("work_switch_off");
            f.tick(100);

            assertSame(external, f.api.pending);
            assertTrue(f.api.sent.isEmpty());
            assertEquals(0, f.next.calls);
            assertFalse(f.coordinator.isBusy());
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void resetPreservesANewerExternalRequestEvenWhenItsItemMatches(boolean sameItem) throws Exception {
        try (var f = new Fixture()) {
            f.coordinator.request(Items.STONE, MaterialRequest.Source.PRINT);
            assertEquals(List.of(Items.STONE), f.api.sent);
            var newer = new ItemStack(sameItem ? Items.STONE : Items.DIRT);
            f.api.pending = newer;

            f.coordinator.reset();

            assertSame(newer, f.api.pending);
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            f.tick(100);
            assertEquals(List.of(Items.STONE), f.api.sent);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"connection", "level"})
    void aScopeChangeCannotClearTheWaitMarkerInTheNewScope(String changed) throws Exception {
        try (var f = new Fixture()) {
            f.coordinator.request(Items.STONE, MaterialRequest.Source.PRINT);
            var pending = f.api.pending;
            if (changed.equals("connection")) {
                set(f.player, LocalPlayer.class, "connection", allocate(CancelledSwapLifecycleTest.Connection.class));
            } else {
                f.client.level = allocate(ClientLevel.class);
            }

            f.resetScope("client_scope_changed");
            f.tick(100);

            assertSame(pending, f.api.pending, "Even an unchanged marker is no longer owned after changing client scope");
            assertEquals(List.of(Items.STONE), f.api.sent);
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
        }
    }

    @Test void disablingItsOwnSentRequestKeepsTheExistingCancelPolicyAndNeverReplaysIt() throws Exception {
        try (var f = new Fixture()) {
            f.coordinator.request(Items.STONE, MaterialRequest.Source.PRINT);
            Configs.Core.WORK_SWITCH.setBooleanValue(false);

            f.resetScope("work_switch_off");

            assertTrue(f.api.pending.isEmpty());
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            // A late server inventory update must not restart a cancelled request.
            f.player.inventory.setItem(10, new ItemStack(Items.STONE, 16));
            f.tick(100);
            assertEquals(List.of(Items.STONE), f.api.sent);
            assertEquals(0, f.next.calls);
            assertFalse(f.coordinator.isBusy());
        }
    }

    @Test void anEmptyBoxRequestWaitsForItsOwnReplyAndNeverAcceptsTheCarriedFilledBox() throws Exception {
        try (var f = new Fixture()) {
            var full = new ItemStack(Items.SHULKER_BOX);
            full.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
            f.player.inventory.setItem(10, full);
            f.api.pending = new ItemStack(Items.DIRT);
            f.coordinator.requestEmptyShulker(Items.SHULKER_BOX);
            f.tick(100);
            f.api.pending = ItemStack.EMPTY;
            f.coordinator.tick();
            assertEquals(List.of(Items.SHULKER_BOX), f.api.sent);
            assertEquals(List.of(true), f.api.emptyOnly);

            // Take It Out may clear this flag because the filled box has the same item type.
            f.api.pending = ItemStack.EMPTY;
            f.tick(60);
            assertTrue(f.coordinator.isBusy());
            assertEquals(0, f.next.calls);
            assertTrue(f.broker.isResourceHeld(ResourceLease.INVENTORY));

            f.player.inventory.setItem(11, new ItemStack(Items.SHULKER_BOX));
            f.coordinator.tick();
            assertFalse(f.coordinator.isBusy());
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertSame(full, f.player.inventory.getItem(10));
            assertEquals(List.of(Items.SHULKER_BOX), f.api.sent);
        }
    }
}
//#endif
