//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.util.List;
import java.util.EnumSet;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialReservation;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseRestockAdapter;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseRestockNetwork;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseRestockPort;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequestCoordinator;
import neko.voidwarehouse.restock.PrinterTakeResultPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.Difficulty;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import net.minecraft.client.player.LocalPlayer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static me.aleksilassila.litematica.printer.integration.quickshulker.CancelledSwapLifecycleTest.set;
import static org.junit.jupiter.api.Assertions.*;

class VoidWarehouseRestockRegressionTest {
    boolean oldEnabled;
    @BeforeAll static void bootstrap() throws Exception { CancelledSwapLifecycleTest.bootstrap(); }
    @BeforeEach void enable() {
        oldEnabled = Configs.Special.VOID_WAREHOUSE_RESTOCK.getBooleanValue();
        Configs.Special.VOID_WAREHOUSE_RESTOCK.setBooleanValue(true);
    }
    @AfterEach void restore() { Configs.Special.VOID_WAREHOUSE_RESTOCK.setBooleanValue(oldEnabled); }

    static class Network implements VoidWarehouseRestockPort {
        int sends;
        int status = PrinterTakeResultPayload.NONE;
        @Override public boolean isSupported() { return true; }
        @Override public int requestTake(List<String> candidates) { sends++; return sends; }
        @Override public int statusOf(int nonce) { return status; }
        @Override public void forget() { status = PrinterTakeResultPayload.NONE; }
    }

    static class Fixture extends TakeItOutRequestOwnershipTest.Fixture {
        final Network network = new Network();
        final VoidWarehouseRestockAdapter warehouse = new VoidWarehouseRestockAdapter(broker, network);
        final MaterialRequestCoordinator materials = new MaterialRequestCoordinator(List.of(warehouse, next));
        final ClientLevel.ClientLevelData clock = new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false);
        final Object previousInventoryClient;
        Fixture() throws Exception {
            set(client.level, Level.class, "levelData", clock);
            set(client.level, ClientLevel.class, "clientLevelData", clock);
            var field = InventoryUtils.class.getDeclaredField("client"); field.setAccessible(true);
            previousInventoryClient = field.get(null);
            setInventoryClient(client);
        }
        private static void setInventoryClient(Object client) throws Exception {
            var access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); access.setAccessible(true);
            var unsafe = (sun.misc.Unsafe) access.get(null);
            var field = InventoryUtils.class.getDeclaredField("client");
            unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), client);
        }
        @Override public void close() throws Exception {
            try { setInventoryClient(previousInventoryClient); } finally { super.close(); }
        }
        void advance(int ticks) { for (int i = 0; i < ticks; i++) materials.tick(); }
        void assertHeld() {
            for (var resource : List.of(ResourceLease.INVENTORY, ResourceLease.CONTAINER, ResourceLease.MAIN_HAND)) {
                assertTrue(broker.isResourceHeld(resource));
                assertFalse(broker.isResourceHeldByOther(resource, "void_warehouse"));
            }
        }
    }

    @Test void anEmptyBoxRequestNeverCompletesWithAStoredFullBox() throws Exception {
        try (var f = new Fixture()) {
            var adapter = new VoidWarehouseRestockAdapter(f.broker);
            var request = new MaterialRequest(1, List.of(Items.SHULKER_BOX), Items.SHULKER_BOX,
                    1, MaterialRequest.Source.PRINT, true);
            var full = new ItemStack(Items.SHULKER_BOX);
            full.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
            f.player.inventory.setItem(10, full);
            // A request already sent; no Fabric networking or game launch is needed to poll it.
            set(adapter, VoidWarehouseRestockAdapter.class, "activeRequest", request);
            set(adapter, VoidWarehouseRestockAdapter.class, "activeNonce", 1);
            VoidWarehouseRestockNetwork.forget();
            assertEquals(MaterialReservation.State.PENDING, adapter.status(request).state());
            f.player.inventory.setItem(11, new ItemStack(Items.SHULKER_BOX));
            assertEquals(MaterialReservation.State.AVAILABLE, adapter.status(request).state());
            assertSame(full, f.player.inventory.getItem(10));
        }
    }

    @Test void completedRestockWaitsForTheOtherInventoryOwnerBeforeSelecting() throws Exception {
        try (var f = new Fixture()) {
            var adapter = new VoidWarehouseRestockAdapter(f.broker);
            var request = new MaterialRequest(2, List.of(Items.STONE), Items.STONE, 1,
                    MaterialRequest.Source.PICK_BLOCK);
            set(adapter, VoidWarehouseRestockAdapter.class, "activeRequest", request);
            set(adapter, VoidWarehouseRestockAdapter.class, "activeNonce", 2);
            f.player.inventory.setItem(3, new ItemStack(Items.STONE));
            f.player.inventory.setSelectedSlot(0);
            assertTrue(f.broker.tryAcquire("container_sync", EnumSet.of(ResourceLease.INVENTORY,
                    ResourceLease.CONTAINER, ResourceLease.MAIN_HAND), 0L));
            assertEquals(MaterialReservation.State.PENDING, adapter.status(request).state());
            assertEquals(0, f.player.inventory.getSelectedSlot());
            assertTrue(f.broker.isResourceHeldByOther(ResourceLease.INVENTORY, "void_warehouse"));
            f.broker.releaseOwner("container_sync");
            assertEquals(MaterialReservation.State.AVAILABLE, adapter.status(request).state());
            assertEquals(3, f.player.inventory.getSelectedSlot());
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
        }
    }

    @Test void waitingForInventoryOwnershipDoesNotSendOrConsumeTheResponseTimeout() throws Exception {
        try (var f = new Fixture()) {
            assertTrue(f.broker.tryAcquire("container_sync", EnumSet.of(ResourceLease.INVENTORY,
                    ResourceLease.CONTAINER, ResourceLease.MAIN_HAND), 0L));
            assertEquals(MaterialReservation.State.PENDING,
                    f.materials.request(Items.STONE, MaterialRequest.Source.PRINT).state());
            f.advance(300);
            assertTrue(f.materials.isBusy());
            assertEquals(0, f.network.sends);
            assertEquals(0, f.next.calls);
            f.broker.releaseOwner("container_sync");
            f.advance(1);
            assertEquals(1, f.network.sends);
            f.assertHeld();
            assertTrue(f.materials.blocksPrinterWhilePending());
            f.network.status = PrinterTakeResultPayload.OK;
            f.advance(90);
            f.assertHeld();
            assertTrue(f.materials.isBusy(), "An OK receipt is not an inventory update");
            f.player.inventory.setItem(10, new ItemStack(Items.STONE, 64));
            f.advance(1);
            assertFalse(f.materials.isBusy());
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertEquals(1, f.network.sends);
            assertEquals(0, f.next.calls);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"work_off", "restock_off", "connection", "level", "timeout"})
    void cancellationNeverSelectsALateDeliveryOrReplaysTheRequest(String reason) throws Exception {
        try (var f = new Fixture()) {
            f.materials.request(Items.STONE, MaterialRequest.Source.PICK_BLOCK);
            f.assertHeld();
            if (reason.equals("restock_off")) {
                Configs.Special.VOID_WAREHOUSE_RESTOCK.setBooleanValue(false);
                f.advance(1);
            } else if (reason.equals("timeout")) {
                f.advance(101);
            } else {
                if (reason.equals("work_off")) Configs.Core.WORK_SWITCH.setBooleanValue(false);
                if (reason.equals("connection")) {
                    set(f.player, LocalPlayer.class, "connection",
                            QuickShulkerPacketContractTest.allocate(CancelledSwapLifecycleTest.Connection.class));
                } else if (reason.equals("level")) {
                    f.client.level = QuickShulkerPacketContractTest.allocate(ClientLevel.class);
                }
                f.materials.onEpochChanged(new RuntimeEvent.EpochChanged(RuntimeEpoch.INITIAL,
                        RuntimeEpoch.INITIAL.next(), reason));
            }
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertFalse(f.materials.isBusy());
            assertTrue(f.broker.tryAcquire("container_sync", EnumSet.of(ResourceLease.INVENTORY), 0L));
            f.player.inventory.setItem(3, new ItemStack(Items.STONE, 64));
            f.network.status = PrinterTakeResultPayload.OK;
            f.advance(300);
            assertEquals(0, f.player.inventory.getSelectedSlot());
            assertEquals(1, f.network.sends);
            assertTrue(f.broker.isResourceHeldByOther(ResourceLease.INVENTORY, "void_warehouse"));
        }
    }

    @Test void aNewWorldWithAnEarlierClockDoesNotInheritTheFailedItemCooldown() throws Exception {
        try (var f = new Fixture()) {
            f.clock.setGameTime(1_000_000L);
            f.materials.request(Items.STONE, MaterialRequest.Source.PRINT);
            f.network.status = PrinterTakeResultPayload.RULE_OFF;
            f.advance(1);
            assertFalse(f.materials.isBusy());
            f.materials.request(Items.STONE, MaterialRequest.Source.PRINT);
            assertEquals(1, f.network.sends, "Same-session failures still need their cooldown");
            f.materials.reset();
            f.clock.setGameTime(10L);
            assertEquals(MaterialReservation.State.PENDING,
                    f.materials.request(Items.STONE, MaterialRequest.Source.PRINT).state());
            assertEquals(2, f.network.sends);
        }
    }
}
//#endif
