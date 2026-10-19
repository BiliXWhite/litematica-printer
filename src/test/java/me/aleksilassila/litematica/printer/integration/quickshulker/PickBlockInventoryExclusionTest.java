//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.util.List;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.integration.containersync.ContainerSyncController;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequestCoordinator;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static me.aleksilassila.litematica.printer.integration.quickshulker.CancelledSwapLifecycleTest.set;
import static me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerPacketContractTest.allocate;
import static org.junit.jupiter.api.Assertions.*;

class PickBlockInventoryExclusionTest {
    @BeforeAll static void bootstrap() throws Exception { CancelledSwapLifecycleTest.bootstrap(); }

    static class Fixture extends TakeItOutRequestOwnershipTest.Fixture {
        final ContainerSyncController sync = new ContainerSyncController(runtime);

        Fixture() throws Exception {
            client.gameMode = allocate(MultiPlayerGameMode.class);
            var info = allocate(PlayerInfo.class);
            set(info, PlayerInfo.class, "gameMode", GameType.SURVIVAL);
            set(player, AbstractClientPlayer.class, "playerInfo", info);
            set(runtime, PrinterRuntime.class, "materialRequests", coordinator);
            var guiField = Minecraft.class.getDeclaredField("gui");
            set(client, Minecraft.class, "gui", allocate(guiField.getType()));
            Configs.Placement.QUICK_SHULKER.setBooleanValue(false);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        void startSync(String phase) throws Exception {
            var type = ContainerSyncController.class.getDeclaredField("phase").getType();
            set(sync, ContainerSyncController.class, "phase", Enum.valueOf((Class) type, phase));
            set(sync, ContainerSyncController.class, "level", client.level);
            set(sync, ContainerSyncController.class, "connection", client.getConnection());
            set(sync, ContainerSyncController.class, "requestedAt", System.nanoTime() / 1_000_000L);
            var acquire = ContainerSyncController.class.getDeclaredMethod("acquire");
            acquire.setAccessible(true);
            assertEquals(true, acquire.invoke(sync));
        }

        void assertSyncStillOwnsInventory() {
            assertTrue(broker.isResourceHeldByOther(ResourceLease.INVENTORY, "take_it_out"));
            assertFalse(broker.isResourceHeldByOther(ResourceLease.INVENTORY, "container_sync"));
            assertTrue(broker.isResourceHeld(ResourceLease.CONTAINER));
            assertTrue(broker.isResourceHeld(ResourceLease.MAIN_HAND));
        }

        void useUnavailableProviders() throws Exception {
            coordinator.reset();
            set(runtime, PrinterRuntime.class, "materialRequests", new MaterialRequestCoordinator(List.of(next)));
        }
    }

    @ParameterizedTest
    @CsvSource({"OPENING, true", "OPENING, false", "RESYNC, true", "RESYNC, false"})
    void failedPickRequestCannotEscapeAStillPendingContainerOperation(String phase, boolean workEnabled) throws Exception {
        try (var f = new Fixture()) {
            f.startSync(phase);
            Configs.Core.WORK_SWITCH.setBooleanValue(workEnabled);
            for (int tick = 0; tick < 300; tick++) f.sync.tick();
            f.assertSyncStillOwnsInventory();

            assertTrue(QuickShulkerBridge.handlePickBlock(f.player, Items.STONE));
            f.tick(10);
            // This input exhausts Take It Out's lease retries and the remaining provider chain.
            // Returning false here lets the native mod dispatch outside the shared broker.
            assertTrue(QuickShulkerBridge.handlePickBlock(f.player, Items.STONE),
                    "UNAVAILABLE cannot allow native extraction while container synchronization owns inventory");
            assertFalse(f.coordinator.isBusy());
            assertEquals(1, f.next.calls);
            assertTrue(f.api.sent.isEmpty());
            f.assertSyncStillOwnsInventory();

            var inventory = f.player.inventoryMenu;
            f.sync.content(0, inventory.getStateId() + 1,
                    inventory.slots.stream().map(slot -> slot.getItem().copy()).toList(), ItemStack.EMPTY);
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            f.useUnavailableProviders();
            assertFalse(QuickShulkerBridge.handlePickBlock(f.player, Items.STONE),
                    "Idle unavailable providers must still leave native pick-block available");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"connection", "level"})
    void changingClientScopeDoesNotLeaveTheNativeFallbackBlocked(String change) throws Exception {
        try (var f = new Fixture()) {
            f.startSync("OPENING");
            f.useUnavailableProviders();
            assertTrue(QuickShulkerBridge.handlePickBlock(f.player, Items.STONE));
            if (change.equals("connection")) {
                set(f.player, LocalPlayer.class, "connection", allocate(CancelledSwapLifecycleTest.Connection.class));
            } else {
                f.client.level = allocate(ClientLevel.class);
            }
            f.sync.tick();
            assertFalse(f.broker.isResourceHeld(ResourceLease.INVENTORY));
            assertFalse(QuickShulkerBridge.handlePickBlock(f.player, Items.STONE));
        }
    }
}
//#endif
