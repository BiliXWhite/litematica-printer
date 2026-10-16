//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import fi.dy.masa.malilib.config.options.ConfigStringList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.FeatureModuleBase;
import me.aleksilassila.litematica.printer.handler.HudStatsManager;
import me.aleksilassila.litematica.printer.handler.handlers.MineHandler;
import me.aleksilassila.litematica.printer.integration.inventory.*;
import me.aleksilassila.litematica.printer.printer.MissingMaterialTracker;
import me.aleksilassila.litematica.printer.printer.PlacementRateController;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.utils.InventorySwitchGuard;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.*;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static me.aleksilassila.litematica.printer.integration.quickshulker.CancelledSwapLifecycleTest.*;
import static me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerPacketContractTest.allocate;
import static org.junit.jupiter.api.Assertions.*;

/** Real trench execution and material selection, with only the world/network boundary replaced. */
class TrenchFillIntegrationTest {
    private static final BlockPos TARGET = new BlockPos(0, 1, 0);
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }

    static class TrenchPlayer extends Player {
        Abilities abilities;
        @Override public Abilities getAbilities() { return abilities; }
        @Override public ItemStack getMainHandItem() { return inventory.getItem(0); }
        @Override public double blockInteractionRange() { return 4.5; }
    }

    static class TrenchLevel extends QuickShulkerPacketContractTest.AuditLevel {
        long tick;
        BlockState targetState;
        @Override public long getGameTime() { return tick; }
        @Override public BlockState getBlockState(BlockPos pos) {
            return pos.equals(TARGET) ? targetState
                    : pos.equals(TARGET.below()) ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
    }

    static class TrenchHandler extends MineHandler {
        TrenchHandler() { super(null); }
        @Override protected boolean canReachIterationPosition(BlockPos pos) { return true; }
    }

    static class Fixture implements AutoCloseable {
        final ConfigStringList config = Configs.Mine.MINE_TRENCH_FILL_BLOCK_LIST;
        final List<String> oldFilters = List.copyOf(config.getStrings());
        final boolean oldAdaptive = Configs.Placement.RTT_ADAPTIVE_INTERVAL.getBooleanValue();
        final Object oldClient = get(null, Minecraft.class, "instance");
        final Object oldRuntime = get(null, RuntimeAccess.class, "current");
        final sun.misc.Unsafe unsafe;
        final java.lang.reflect.Field playerUtilsClient;
        final Object oldPlayerUtilsClient;
        final Minecraft client = allocate(Minecraft.class);
        final TrenchPlayer player = allocate(TrenchPlayer.class);
        final TrenchLevel level = allocate(TrenchLevel.class);
        final MineHandler handler = allocate(TrenchHandler.class);
        final Object state;
        final InventorySwitchGuard switchGuard;
        final PlacementRateController rate = new PlacementRateController(null);
        final List<MaterialRequest> requests = new ArrayList<>();
        final List<Item[]> queued = new ArrayList<>();
        ActionPort.SendResult result = ActionPort.SendResult.SENT;

        Fixture() throws Exception {
            config.setStrings(List.of("minecraft:cobblestone"));
            Configs.Placement.RTT_ADAPTIVE_INTERVAL.setBooleanValue(false);
            player.inventory = new Inventory(player, new EntityEquipment());
            player.abilities = new Abilities();
            set(player, Entity.class, "position", Vec3.ZERO);
            level.tick = 100;
            level.targetState = Blocks.LAVA.defaultBlockState();
            client.player = player;
            client.level = level;
            set(null, Minecraft.class, "instance", client);
            var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            playerUtilsClient = PlayerUtils.class.getDeclaredField("client");
            playerUtilsClient.setAccessible(true);
            oldPlayerUtilsClient = playerUtilsClient.get(null);
            setPlayerUtilsClient(client);
            switchGuard = new InventorySwitchGuard(client, () -> level.tick);
            var runtime = allocate(PrinterRuntime.class);
            set(runtime, PrinterRuntime.class, "inventorySwitchGuard", switchGuard);
            set(runtime, PrinterRuntime.class, "materialRequests", new MaterialRequestCoordinator(List.of(new InventoryProvider() {
                @Override public String id() { return "trench-test"; }
                @Override public MaterialReservation request(MaterialRequest request) {
                    requests.add(request);
                    return new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
                }
            })));
            RuntimeAccess.install(runtime);
            var constructor = Class.forName("me.aleksilassila.litematica.printer.handler.handlers.TrenchFillState").getDeclaredConstructor();
            constructor.setAccessible(true);
            state = constructor.newInstance();
            set(handler, MineHandler.class, "trenchFill", state);
            set(handler, FeatureModuleBase.class, "level", level);
            set(handler, FeatureModuleBase.class, "player", player);
            set(handler, FeatureModuleBase.class, "placementRateController", rate);
            set(handler, FeatureModuleBase.class, "hudStats", new HudStatsManager(client, () -> level.tick, null));
            set(handler, FeatureModuleBase.class, "missingMaterials", new MissingMaterialTracker());
            var actions = (ActionPort) Proxy.newProxyInstance(ActionPort.class.getClassLoader(), new Class<?>[]{ActionPort.class}, (p,m,a) -> {
                if (m.getName().equals("queueClick")) {
                    assertEquals(TARGET.below(), a[0]);
                    assertEquals(Direction.UP, a[1]);
                    queued.add(((Item[]) a[5]).clone());
                    return true;
                }
                if (m.getName().equals("sendQueue")) return result;
                return m.getReturnType() == boolean.class ? false : null;
            });
            set(handler, FeatureModuleBase.class, "actionBroker", actions);
        }

        boolean execute() throws Exception {
            var method = MineHandler.class.getDeclaredMethod("executeTrenchFill", BlockPos.class, AtomicReference.class);
            method.setAccessible(true);
            var skip = new AtomicReference<>(false);
            method.invoke(handler, TARGET, skip);
            return skip.get();
        }

        boolean canAttempt(long tick) throws Exception {
            var method = state.getClass().getDeclaredMethod("canAttempt", BlockPos.class, long.class);
            method.setAccessible(true);
            return (boolean) method.invoke(state, TARGET, tick);
        }

        @SuppressWarnings("removal")
        void setPlayerUtilsClient(Object value) {
            unsafe.putObject(unsafe.staticFieldBase(playerUtilsClient), unsafe.staticFieldOffset(playerUtilsClient), value);
        }

        @Override public void close() throws Exception {
            config.setStrings(oldFilters);
            Configs.Placement.RTT_ADAPTIVE_INTERVAL.setBooleanValue(oldAdaptive);
            setPlayerUtilsClient(oldPlayerUtilsClient);
            set(null, Minecraft.class, "instance", oldClient);
            set(null, RuntimeAccess.class, "current", oldRuntime);
        }
    }

    @Test void cobblestoneQueuesAgainstSoulSandAndWaitsForTheWorld() throws Exception {
        try (var f = new Fixture()) {
            f.player.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            f.player.inventory.setItem(1, new ItemStack(Items.SAND, 64));
            assertFalse(f.execute());
            assertEquals(1, f.queued.size());
            assertArrayEquals(new Item[]{Items.COBBLESTONE}, f.queued.getFirst());
            assertEquals(100L, f.rate.lastSentTick());
            assertFalse(f.canAttempt(130L));
            assertEquals(Blocks.LAVA.defaultBlockState(), f.level.targetState, "No speculative world write");
            assertEquals(64, f.player.getMainHandItem().getCount(), "No direct inventory mutation");
            // Changing the list must not release an already queued placement's confirmation.
            f.config.setStrings(List.of("minecraft:stone"));
            var refresh = MineHandler.class.getDeclaredMethod("refreshTrenchFillMaterials");
            refresh.setAccessible(true);
            refresh.invoke(f.handler);
            assertFalse(f.canAttempt(130L));
            assertArrayEquals(new Item[]{Items.COBBLESTONE}, f.queued.getFirst());
        }
    }

    @Test void missingCobblestoneRequestsOnlyCobblestoneEvenWhenSandIsHeld() throws Exception {
        try (var f = new Fixture()) {
            f.player.inventory.setItem(0, new ItemStack(Items.SAND, 64));
            assertTrue(f.execute());
            assertTrue(f.queued.isEmpty());
            assertEquals(1, f.requests.size());
            assertEquals(List.of(Items.COBBLESTONE), f.requests.getFirst().acceptedItems());
            assertFalse(f.canAttempt(119L));
            assertTrue(f.canAttempt(120L));
        }
    }

    @Test void emptyOrInvalidListsNeverRequestOrPlaceSand() throws Exception {
        try (var f = new Fixture()) {
            f.player.inventory.setItem(0, new ItemStack(Items.SAND, 64));
            for (var filters : List.of(List.<String>of(), List.of("minecraft:stick"))) {
                f.config.setStrings(filters);
                assertTrue(f.execute());
                assertTrue(f.queued.isEmpty());
                assertTrue(f.requests.isEmpty());
                f.level.tick += 20;
            }
        }
    }

    @Test void pendingInventorySwitchDoesNotQueueAPlacement() throws Exception {
        try (var f = new Fixture()) {
            f.player.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            f.switchGuard.markSwitchIfNeeded(Items.COBBLESTONE);
            assertTrue(f.execute());
            assertTrue(f.queued.isEmpty());
            assertTrue(f.requests.isEmpty());
            assertEquals(Long.MIN_VALUE, f.rate.lastSentTick());
        }
    }

    @Test void waitingForLookDoesNotCountAsSent() throws Exception {
        try (var f = new Fixture()) {
            f.player.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            f.result = ActionPort.SendResult.WAITING_FOR_LOOK;
            assertTrue(f.execute());
            assertEquals(1, f.queued.size());
            assertFalse(f.canAttempt(101L));
            assertEquals(Long.MIN_VALUE, f.rate.lastSentTick());
        }
    }
}
//#endif
