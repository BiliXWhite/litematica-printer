//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.runtime.*;
import me.aleksilassila.litematica.printer.runtime.*;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the real adapter/controller/disabled bridge with a captured network and a headless player. */
class CancelledSwapLifecycleTest {
    @BeforeAll static void bootstrap() throws Exception {
        InventoryRegressionTest.bootstrap();
        Class.forName("me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils");
    }
    static class Connection extends ClientPacketListener {
        List<Packet<?>> packets;
        Connection() { super(null, null, null); }
        @Override public void send(Packet<?> packet) { packets.add(packet); }
    }
    static class Player extends LocalPlayer {
        net.minecraft.world.entity.player.Inventory inventory;
        boolean closed;
        Player() { super(null,null,null,null,null,null,false,null); }
        @Override public net.minecraft.world.entity.player.Inventory getInventory() { return inventory; }
        @Override public void closeContainer() { closed = true; containerMenu = inventoryMenu; }
    }
    static void set(Object instance, Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value);
    }
    static Object get(Object instance, Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(instance);
    }
    static class Fixture implements AutoCloseable {
        Object oldClient = get(null, Minecraft.class, "instance");
        Object oldRuntime = get(null, RuntimeAccess.class, "current");
        boolean oldQuick = Configs.Placement.QUICK_SHULKER.getBooleanValue();
        boolean oldWork = Configs.Core.WORK_SWITCH.getBooleanValue();
        boolean oldOrderly = Configs.Placement.STORE_ORDERLY.getBooleanValue();
        Minecraft client = QuickShulkerPacketContractTest.allocate(Minecraft.class);
        Player player = QuickShulkerPacketContractTest.allocate(Player.class);
        Connection connection = QuickShulkerPacketContractTest.allocate(Connection.class);
        AtomicBoolean leased = new AtomicBoolean();
        QuickShulkerAdapter adapter;
        QuickShulkerRequestController requests;
        AbstractContainerMenu menu;
        Fixture() throws Exception {
            connection.packets = new ArrayList<>();
            set(player, LocalPlayer.class, "connection", connection);
            player.inventory = new net.minecraft.world.entity.player.Inventory(player, new net.minecraft.world.entity.EntityEquipment());
            set(player, net.minecraft.world.entity.player.Player.class, "inventoryMenu", new InventoryMenu(player.inventory, true, player));
            menu = new QuickShulkerPacketContractTest.ServerMenu(new net.minecraft.world.SimpleContainer(1), player.inventory);
            menu.slots.get(0).set(new ItemStack(Items.STONE, 64));
            player.containerMenu = menu;
            client.player = player;
            set(null, Minecraft.class, "instance", client);
            var port = (ActionPort) Proxy.newProxyInstance(ActionPort.class.getClassLoader(), new Class<?>[]{ActionPort.class}, (p,m,a) -> {
                if (m.getName().equals("tryAcquire")) { leased.set(true); return true; }
                if (m.getName().equals("releaseOwner")) { leased.set(false); return null; }
                return m.getReturnType() == boolean.class ? false : null;
            });
            adapter = new QuickShulkerAdapter(port);
            requests = (QuickShulkerRequestController) get(adapter, QuickShulkerAdapter.class, "requests");
            var runtime = QuickShulkerPacketContractTest.allocate(PrinterRuntime.class);
            set(runtime, PrinterRuntime.class, "quickShulkerAdapter", adapter);
            RuntimeAccess.install(runtime);
            Configs.Placement.QUICK_SHULKER.setBooleanValue(true);
            Configs.Core.WORK_SWITCH.setBooleanValue(true);
            Configs.Placement.STORE_ORDERLY.setBooleanValue(false);
            set(requests, QuickShulkerRequestController.class, "automatedContainerId", 7);
            set(requests, QuickShulkerRequestController.class, "isOpenHandler", true);
            var begin = QuickShulkerRequestController.class.getDeclaredMethod("beginSwap", AbstractContainerMenu.class, int.class, int.class, int.class);
            begin.setAccessible(true); begin.invoke(requests, menu, 0, 1, 2);
        }
        @Override public void close() throws Exception {
            set(null, Minecraft.class, "instance", oldClient);
            set(null, RuntimeAccess.class, "current", oldRuntime);
            Configs.Placement.QUICK_SHULKER.setBooleanValue(oldQuick);
            Configs.Core.WORK_SWITCH.setBooleanValue(oldWork);
            Configs.Placement.STORE_ORDERLY.setBooleanValue(oldOrderly);
        }
    }

    @ParameterizedTest @ValueSource(strings={"timeout", "quick_off", "work_off"})
    void delayedSnapshotDrainsWhileDisabledAndNeverReplaysSwap(String trigger) throws Exception {
        try (var f = new Fixture()) {
            if (trigger.equals("timeout")) {
                set(f.requests, QuickShulkerRequestController.class, "openHandlerTimeout", 1);
                f.adapter.tick();
            } else if (trigger.equals("quick_off")) {
                Configs.Placement.QUICK_SHULKER.setBooleanValue(false);
                f.adapter.reset();
            } else {
                Configs.Core.WORK_SWITCH.setBooleanValue(false);
                f.adapter.onEpochChanged(new RuntimeEvent.EpochChanged(RuntimeEpoch.INITIAL, RuntimeEpoch.INITIAL.next(), "work_switch_off"));
            }
            assertTrue(f.requests.isCancelling());
            assertFalse(f.player.closed);
            for (int tick = 0; tick < 300; tick++) QuickShulkerBridge.onTick();
            assertTrue(f.leased.get());
            assertFalse(f.player.closed);
            assertEquals(2, f.connection.packets.size(), "Only the original swap and one no-op resync");
            var sync = (ServerboundContainerClickPacket) f.connection.packets.get(1);
            assertEquals(-1, sync.stateId());
            assertEquals(-1, sync.slotNum());
            assertEquals(ContainerInput.PICKUP, sync.containerInput());
            var items = List.of(ItemStack.EMPTY, new ItemStack(Items.STONE,64));
            f.menu.initializeContents(9, items, ItemStack.EMPTY); // vanilla applies before the TAIL callback
            QuickShulkerBridge.onInventoryContent(7, items, ItemStack.EMPTY);
            assertTrue(f.player.closed);
            assertFalse(f.leased.get());
            assertFalse(f.requests.isCancelling());
            assertEquals(64, f.player.inventory.getItem(2).getCount());
        }
    }

    @Test void rejectionSnapshotAlsoFinishesCancellation() throws Exception {
        try (var f = new Fixture()) {
            f.adapter.reset();
            var unchanged = List.of(new ItemStack(Items.STONE,64), ItemStack.EMPTY);
            f.menu.initializeContents(9, unchanged, ItemStack.EMPTY);
            f.adapter.onInventoryContent(7, unchanged, ItemStack.EMPTY);
            assertTrue(f.player.closed);
            assertFalse(f.leased.get());
            assertTrue(f.player.inventory.getItem(2).isEmpty());
        }
    }

    @Test void changingServerDiscardsTheOldDrainWithoutClosingTheNewMenu() throws Exception {
        try (var f = new Fixture()) {
            f.adapter.reset();
            var replacement = new InventorySyncDrainTest.Menu(7);
            f.player.containerMenu = replacement;
            f.leased.set(false); // broker resets before the adapter at an epoch change
            f.adapter.onEpochChanged(new RuntimeEvent.EpochChanged(RuntimeEpoch.INITIAL, RuntimeEpoch.INITIAL.next(), "client_scope_changed"));
            assertFalse(f.requests.isCancelling());
            assertFalse(f.leased.get());
            f.adapter.onInventoryContent(7, List.of(ItemStack.EMPTY), ItemStack.EMPTY);
            assertSame(replacement, f.player.containerMenu);
            assertFalse(f.player.closed);
            assertEquals(2, f.connection.packets.size());
        }
    }
}
//#endif
