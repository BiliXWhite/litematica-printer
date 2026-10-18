package me.aleksilassila.litematica.printer.integration.containersync;

import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.action.ActionBroker;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import static org.junit.jupiter.api.Assertions.*;

class ContainerSyncControllerTest {
    @BeforeAll static void bootstrap() { ContainerSyncPlanTest.bootstrap(); }

    @Test void closeBeforeInitialContentsWaitsForTheOutstandingOpen() throws Exception {
        try (var f = new Fixture()) {
            assertTrue(f.controller.guardClose(), "Closing now would discard the only expected content reply");
            assertEquals(true, get(f.controller, "cancelling"));
            assertFalse(f.otherInventoryOwner());
        }
    }

    @Test void clicksBeforeInitialContentsCannotRaceTheCancellationClose() throws Exception {
        try (var f = new Fixture()) {
            assertTrue(f.controller.guardManualInput());
            assertEquals(true, get(f.controller, "cancelling"));
            assertFalse(f.otherInventoryOwner());
        }
    }

    @Test void serverClosingBeforeInitialContentsReleasesTheBatchWithoutTouchingReplacement() throws Exception {
        try (var f = new Fixture()) {
            var replacement = new Menu(0);
            f.client.player.containerMenu = replacement;
            f.controller.tick();
            assertEquals("PAUSED", get(f.controller, "phase").toString());
            assertTrue(f.otherInventoryOwner());
            assertSame(replacement, f.client.player.containerMenu);
            f.controller.content(7, 1, java.util.List.of(), ItemStack.EMPTY);
            assertEquals("PAUSED", get(f.controller, "phase").toString());
        }
    }

    @Test void disabledWhileInitialContentsAreDelayedKeepsInventoryExclusive() throws Exception {
        try (var f = new Fixture()) {
            me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH.setBooleanValue(false);
            set(f.controller, "requestedAt", 0L);
            for (int tick = 0; tick < 100; tick++) f.controller.tick();
            assertEquals("CONTENT", get(f.controller, "phase").toString());
            assertEquals(true, get(f.controller, "cancelling"));
            assertFalse(f.otherInventoryOwner());
            assertTrue(f.controller.guardClose());
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"connection", "level"})
    void scopeChangeDiscardsCancelledOpenAndIgnoresLateContentsWithReusedId(String change) throws Exception {
        try (var f = new Fixture()) {
            f.controller.guardClose();
            if (change.equals("connection")) {
                set(f.client.player, LocalPlayer.class, "connection",
                        allocate(net.minecraft.client.multiplayer.ClientPacketListener.class));
            } else f.client.level = allocate(net.minecraft.client.multiplayer.ClientLevel.class);
            var replacement = new Menu(7);
            f.client.player.containerMenu = replacement;
            f.controller.tick();
            assertEquals("IDLE", get(f.controller, "phase").toString());
            assertTrue(f.otherInventoryOwner());
            f.controller.content(7, 1, java.util.List.of(), ItemStack.EMPTY);
            assertEquals("IDLE", get(f.controller, "phase").toString());
            assertSame(replacement, f.client.player.containerMenu);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"pickup", "insertion", "return"})
    void forcedCloseWithClickInFlightWaitsForReplacementInventory(String step) throws Exception {
        try (var f = new Fixture()) {
            var port = f.startTransfer(step);
            var owned = f.client.player.containerMenu;
            int clicks = port.clicks.size();
            var inventory = new Menu(0);
            f.client.player.containerMenu = inventory;
            f.controller.tick();
            assertFalse(f.otherInventoryOwner(), "A closed screen does not confirm the in-flight click or cursor return");
            me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH.setBooleanValue(false);
            set(f.controller, "requestedAt", 0L);
            for (int i = 0; i < 100; i++) f.controller.tick();
            assertEquals(clicks, port.clicks.size(), "Never replay an old menu slot against inventory menu 0");
            assertFalse(f.otherInventoryOwner());
            f.controller.content(owned.containerId, 99, java.util.List.of(), ItemStack.EMPTY);
            f.controller.content(0, 0, java.util.List.of(), ItemStack.EMPTY);
            f.controller.content(0, 1, java.util.List.of(ItemStack.EMPTY), ItemStack.EMPTY);
            assertFalse(f.otherInventoryOwner(), "Ignore old-menu, stale-revision and incomplete snapshots");
            f.controller.content(0, 1, java.util.List.of(), ItemStack.EMPTY);
            assertTrue(f.otherInventoryOwner());
            assertSame(inventory, f.client.player.containerMenu);
            assertEquals("PAUSED", get(f.controller, "phase").toString());
        }
    }

    @Test void forcedCloseWithNonemptyAuthoritativeCursorRequiresConfirmedManualRecovery() throws Exception {
        try (var f = new Fixture()) {
            var port = f.startTransfer("insertion");
            var inventory = new Menu(0);
            f.client.player.containerMenu = inventory;
            f.controller.tick();
            inventory.setCarried(ContainerSyncPlanTest.stone(4));
            f.controller.content(0, 1, java.util.List.of(), inventory.getCarried());
            assertFalse(f.otherInventoryOwner());
            assertTrue(f.controller.guardClose(), "Closing with an unsettled cursor could invoke vanilla dropping");
            assertFalse(f.controller.guardManualInput(), "Let the user put down the authoritative cursor");
            // Client prediction and an earlier full reply cannot acknowledge this new manual click.
            inventory.setCarried(ItemStack.EMPTY);
            f.controller.content(0, 2, java.util.List.of(), ItemStack.EMPTY);
            assertFalse(f.otherInventoryOwner());
            f.controller.tick();
            f.controller.content(0, 3, java.util.List.of(), ItemStack.EMPTY);
            assertTrue(f.otherInventoryOwner());
            assertEquals(java.util.List.of(1, 0), port.clicks);
        }
    }

    @Test void replacingMenuDuringForcedCloseDrainRejectsThePreviousReply() throws Exception {
        try (var f = new Fixture()) {
            f.startTransfer("pickup");
            f.client.player.containerMenu = new Menu(0);
            f.controller.tick();
            var manual = new Menu(9);
            f.client.player.containerMenu = manual;
            f.controller.content(0, 1, java.util.List.of(), ItemStack.EMPTY);
            assertFalse(f.otherInventoryOwner());
            f.controller.tick();
            f.controller.content(0, 2, java.util.List.of(), ItemStack.EMPTY);
            assertFalse(f.otherInventoryOwner());
            f.controller.content(9, 1, java.util.List.of(), ItemStack.EMPTY);
            assertTrue(f.otherInventoryOwner());
            assertSame(manual, f.client.player.containerMenu);
        }
    }

    @Test void disablingEpochRetainsForcedCloseDrainButChangingServerDiscardsIt() throws Exception {
        try (var f = new Fixture()) {
            f.startTransfer("pickup");
            f.client.player.containerMenu = new Menu(0);
            f.controller.tick();
            var event = new me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent.EpochChanged(
                    RuntimeEpoch.INITIAL, RuntimeEpoch.INITIAL.next(), "work_switch_disabled");
            f.broker.onEpochChanged(event);
            set(f.client, "gameThread", Thread.currentThread());
            f.controller.onEpochChanged(event);
            assertFalse(f.otherInventoryOwner());
            set(f.client.player, LocalPlayer.class, "connection",
                    allocate(net.minecraft.client.multiplayer.ClientPacketListener.class));
            var replacement = new Menu(0);
            f.client.player.containerMenu = replacement;
            f.controller.tick();
            f.controller.content(0, 1, java.util.List.of(), ItemStack.EMPTY);
            assertEquals("IDLE", get(f.controller, "phase").toString());
            assertTrue(f.otherInventoryOwner());
            assertSame(replacement, f.client.player.containerMenu);
        }
    }

    static class Menu extends AbstractContainerMenu {
        Menu(int id) { super(null, id); }
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    }

    static class Fixture implements AutoCloseable {
        final Object oldClient = get(null, Minecraft.class, "instance");
        final boolean oldWork = me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH.getBooleanValue();
        final Minecraft client = allocate(Minecraft.class);
        final PrinterRuntime runtime = allocate(PrinterRuntime.class);
        final ActionBroker broker = new ActionBroker(runtime, new ActionManager());
        final ContainerSyncController controller = new ContainerSyncController(runtime);

        Fixture() throws Exception {
            set(runtime, "epoch", RuntimeEpoch.INITIAL);
            set(runtime, "actionBroker", broker);
            set(controller, "client", client);
            client.player = allocate(LocalPlayer.class);
            client.player.containerMenu = new Menu(7);
            var guiField = Minecraft.class.getDeclaredField("gui");
            Object gui = allocate(guiField.getType());
            set(client, "gui", gui);
            //#if MC > 260100
            //$$ var hudField = gui.getClass().getDeclaredField("hud");
            //$$ set(gui, "hud", allocate(hudField.getType()));
            //#endif
            set(null, Minecraft.class, "instance", client);
            me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH.setBooleanValue(true);
            phase("CONTENT");
            set(controller, "candidate", 7);
            set(controller, "requestedAt", System.nanoTime() / 1_000_000L);
            assertEquals(true, invoke(controller, "acquire"));
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        void phase(String phase) throws Exception {
            var type = ContainerSyncController.class.getDeclaredField("phase").getType();
            set(controller, "phase", Enum.valueOf((Class) type, phase));
        }
        boolean otherInventoryOwner() {
            return broker.tryAcquire("quick_shulker", EnumSet.of(ResourceLease.INVENTORY, ResourceLease.CONTAINER), 0);
        }
        ContainerSyncTransferTest.Port startTransfer(String step) throws Exception {
            var owned = client.player.containerMenu;
            var port = new ContainerSyncTransferTest.Port() {
                @Override public Object context() { return client.player.containerMenu == owned ? owned : null; }
                @Override public boolean acquire() { return true; }
                @Override public void release() { }
            };
            var plan = ContainerSyncTransferTest.plan();
            var transfer = new ContainerSyncTransfer(port, plan);
            set(controller, "menu", owned);
            set(controller, "transfer", transfer);
            phase("TRANSFER");
            transfer.tick(0, true);
            if (!step.equals("pickup")) {
                transfer.observe(owned, plan.steps().get(0).items(), plan.steps().get(0).cursor());
                owned.setCarried(plan.steps().get(0).cursor().copy());
                transfer.tick(1, true);
            }
            if (step.equals("return")) {
                transfer.observe(owned, plan.steps().get(1).items(), plan.steps().get(1).cursor());
                controller.guardClose();
                transfer.tick(2, false);
            }
            return port;
        }
        @Override public void close() throws Exception {
            set(null, Minecraft.class, "instance", oldClient);
            me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH.setBooleanValue(oldWork);
        }
    }

    @Test void closingOneMenuRetainsBatchLeaseUntilCancellation() throws Exception {
        // Allocate only headless state holders; exercise the real controller and ActionBroker.
        var runtime = allocate(PrinterRuntime.class);
        set(runtime, "epoch", RuntimeEpoch.INITIAL);
        var broker = new ActionBroker(runtime, new ActionManager());
        set(runtime, "actionBroker", broker);
        var controller = allocate(ContainerSyncController.class);
        set(controller, "runtime", runtime);
        set(controller, "client", allocate(Minecraft.class));
        set(controller, "remaining", new LinkedHashSet<>());

        assertEquals(true, invoke(controller, "acquire"));
        invoke(controller, "closeOwned");
        assertFalse(broker.tryAcquire("print", EnumSet.of(ResourceLease.LOOK, ResourceLease.MAIN_HAND, ResourceLease.INTERACTION), 0));
        assertFalse(broker.tryAcquire("quick_shulker", EnumSet.of(ResourceLease.INVENTORY, ResourceLease.CONTAINER), 0));
        assertFalse(broker.tryAcquire("take_it_out", EnumSet.of(ResourceLease.INVENTORY), 0));

        invoke(controller, "discard");
        assertTrue(broker.tryAcquire("print", EnumSet.of(ResourceLease.MAIN_HAND, ResourceLease.INTERACTION), 0));
        assertTrue(broker.tryAcquire("take_it_out", EnumSet.of(ResourceLease.INVENTORY), 0));
    }

    static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }
    private static void set(Object instance, String name, Object value) throws Exception {
        set(instance, instance.getClass(), name, value);
    }
    static void set(Object instance, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true); field.set(instance, value);
    }
    private static Object get(Object instance, String name) throws Exception {
        return get(instance, instance.getClass(), name);
    }
    private static Object get(Object instance, Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true); return field.get(instance);
    }
    private static Object invoke(Object instance, String name) throws Exception {
        var method = instance.getClass().getDeclaredMethod(name);
        method.setAccessible(true); return method.invoke(instance);
    }
}
