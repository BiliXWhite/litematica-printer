//#if MC == 11802 || MC == 12001 || MC == 12104 || MC == 260100 || MC == 260300
package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interaction.*;
import me.aleksilassila.litematica.printer.integration.tweakeroo.TweakerooToolSwitchPort;
import me.aleksilassila.litematica.printer.mixin.printer.mc.MixinClientLevel;
import me.aleksilassila.litematica.printer.utils.InventorySwitchGuard;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PredictiveAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.*;
//#if MC > 11802
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
//#endif
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import me.aleksilassila.litematica.printer.mixin_extension.*;
import me.aleksilassila.litematica.printer.runtime.*;
import me.aleksilassila.litematica.printer.handler.*;
import me.aleksilassila.litematica.printer.handler.handlers.MineHandler;
import me.aleksilassila.litematica.printer.handler.handlers.PrintHandler;
import me.aleksilassila.litematica.printer.handler.handlers.print.*;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.core.runtime.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Real mining controller, prediction bridge and vanilla cancellation; headless world/network boundaries. */
class MiningInteractionRegressionTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }
    static final BlockPos TARGET = new BlockPos(2, 64, 2);
    static final sun.misc.Unsafe U;
    static {
        try { var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); U = (sun.misc.Unsafe) f.get(null); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    static <T> T alloc(Class<T> t) throws Exception { return t.cast(U.allocateInstance(t)); }
    @SuppressWarnings("removal")
    static void set(Object o, Class<?> t, String n, Object v) throws Exception {
        var f = t.getDeclaredField(n); f.setAccessible(true);
        if (Modifier.isStatic(f.getModifiers())) U.putObject(U.staticFieldBase(f), U.staticFieldOffset(f), v);
        else f.set(o, v);
    }
    static Object get(Object o, Class<?> t, String n) throws Exception {
        var f = t.getDeclaredField(n); f.setAccessible(true); return f.get(o);
    }
    static class Player extends LocalPlayer {
        //#if MC >= 260300
        //$$ Player() { super(null, null, null, null, null, null, false, null, null); }
        //#elseif MC >= 260100
        Player() { super(null, null, null, null, null, null, false, null); }
        //#else
        //$$ Player() { super(null, null, null, null, null, false, false); }
        //#endif
        float speed;
        Abilities abilities;
        Inventory inventory;
        ItemStack held;
        @Override public Abilities getAbilities() { return abilities; }
        @Override public Inventory getInventory() { return inventory; }
        @Override public ItemStack getMainHandItem() { return held; }
        @Override public boolean blockActionRestricted(net.minecraft.world.level.Level level, BlockPos pos,
                net.minecraft.world.level.GameType mode) { return false; }
        @Override public float getDestroySpeed(BlockState state) { return speed; }
        @Override public boolean hasCorrectToolForDrops(BlockState state) { return true; }
        @Override public void resetAttackStrengthTicker() { }
    }
    static class Connection extends ClientPacketListener {
        //#if MC >= 12006
        Connection() { super(null, null, null); }
        //#elseif MC >= 12001
        //$$ Connection() { super(null, null, null, null, null, null); }
        //#else
        //$$ Connection() { super(null, null, null, null, null); }
        //#endif
        List<ServerboundPlayerActionPacket> packets;
        @Override public void send(Packet<?> p) { if (p instanceof ServerboundPlayerActionPacket a) packets.add(a); }
    }
    static class Sounds extends SoundManager {
        //#if MC >= 12001
        Sounds() { super(null); }
        //#else
        //$$ Sounds() { super(null, null); }
        //#endif
        //#if MC >= 260100
        @Override public SoundEngine.PlayResult play(SoundInstance sound) { return null; }
        //#else
        //$$ @Override public void play(SoundInstance sound) { }
        //#endif
    }
    static class Tutorial extends net.minecraft.client.tutorial.Tutorial {
        Tutorial() { super(null, null); }
        @Override public void onDestroyBlock(ClientLevel level, BlockPos pos, BlockState state, float progress) { }
    }
    static class PredictionBridge extends MixinClientLevel { }
    static class Level extends ClientLevel implements NetworkUtils.SequenceExtension {
        //#if MC >= 12104
        Level() { super(null, null, null, null, 0, 0, null, false, 0, 0); }
        //#else
        //$$ Level() { super(null, null, null, null, 0, 0, null, null, false, 0L); }
        //#endif
        BlockState state;
        long tick;
        WorldBorder border;
        //#if MC > 11802
        BlockStatePredictionHandler predictions;
        //#endif
        PredictionBridge bridge;
        Player player;
        @Override public BlockState getBlockState(BlockPos pos) { return state; }
        @Override public WorldBorder getWorldBorder() { return border; }
        @Override public long getGameTime() { return tick; }
        @Override public void destroyBlockProgress(int id, BlockPos pos, int stage) { }
        //#if MC >= 260300
        //$$ @Override public void addBreakingBlockEffects(BlockPos pos, Direction direction, boolean sound) { }
        //#endif
        //#if MC > 11802
        @Override public void syncBlockState(BlockPos pos, BlockState authoritative, Vec3 oldPosition) { state = authoritative; }
        //#endif
        @Override public Packet<ServerGamePacketListener> litematica_printer3$predict(PredictiveAction action) {
            return bridge.litematica_printer3$predict(action);
        }
        void predictDestroy(BlockPos pos) {
            // Same retain-before-change contract confirmed from ClientLevel.setBlock bytecode.
            //#if MC > 11802
            if (predictions.isPredicting()) predictions.retainKnownServerState(pos, state, player);
            //#endif
            state = Blocks.AIR.defaultBlockState();
        }
    }
    static class Port implements MiningInteractionPort {
        Minecraft client;
        BlockPos pos = BlockPos.ZERO;
        ItemStack item = ItemStack.EMPTY;
        float progress;
        boolean destroying;
        int localDestroys;
        public Minecraft client() { return client; }
        public BlockPos destroyPos() { return pos; }
        public void destroyPos(BlockPos p) { pos = p; }
        public ItemStack destroyingItem() { return item; }
        public void destroyingItem(ItemStack i) { item = i; }
        public float destroyProgress() { return progress; }
        public void destroyProgress(float p) { progress = p; }
        public boolean isDestroying() { return destroying; }
        public void isDestroying(boolean d) { destroying = d; }
        public boolean destroyBlock(BlockPos p) { localDestroys++; ((Level) client.level).predictDestroy(p); return true; }
        public boolean matchesDestroyTarget(BlockPos p) {
            try {
                set(client.gameMode, MultiPlayerGameMode.class, "destroyBlockPos", pos);
                set(client.gameMode, MultiPlayerGameMode.class, "destroyingItem", item);
                var method = MultiPlayerGameMode.class.getDeclaredMethod("sameDestroyTarget", BlockPos.class);
                method.setAccessible(true);
                return (boolean) method.invoke(client.gameMode, p);
            } catch (Exception e) { throw new AssertionError(e); }
        }
        public boolean isInventorySwitchPending() { return false; }
        public void ensureCarriedItemSent() { }
        public ServerboundPlayerActionPacket actionPacket(ServerboundPlayerActionPacket.Action a, BlockPos p, Direction d, int s) {
            //#if MC > 11802
            return new ServerboundPlayerActionPacket(a, p, d, s);
            //#else
            //$$ return new ServerboundPlayerActionPacket(a, p, d);
            //#endif
        }
    }
    static class GameMode extends MultiPlayerGameMode implements MiningInteractionPort, MultiPlayerGameModeExtension {
        GameMode() { super(null, null); }
        Port port;
        MiningInteractionController controller;
        @Override public Minecraft client() { return port.client(); }
        @Override public BlockPos destroyPos() { return port.destroyPos(); }
        @Override public void destroyPos(BlockPos p) { port.destroyPos(p); }
        @Override public ItemStack destroyingItem() { return port.destroyingItem(); }
        @Override public void destroyingItem(ItemStack i) { port.destroyingItem(i); }
        @Override public float destroyProgress() { return port.destroyProgress(); }
        @Override public void destroyProgress(float p) { port.destroyProgress(p); }
        @Override public boolean isDestroying() { return port.isDestroying(); }
        @Override public void isDestroying(boolean d) { port.isDestroying(d); }
        @Override public boolean destroyBlock(BlockPos p) { return port.destroyBlock(p); }
        @Override public boolean matchesDestroyTarget(BlockPos p) { return port.matchesDestroyTarget(p); }
        @Override public boolean isInventorySwitchPending() { return false; }
        @Override public void ensureCarriedItemSent() { }
        @Override public ServerboundPlayerActionPacket actionPacket(ServerboundPlayerActionPacket.Action a, BlockPos p, Direction d, int s) {
            return port.actionPacket(a, p, d, s);
        }
        @Override public void litematica_printer$resetRuntime() { controller.reset(); }
        @Override public BlockBreakResult litematica_printer$continueDestroyBlock(boolean predict, BlockPos p, Direction d,
                boolean delayed, boolean tool) { return controller.continueDestroy(predict, p, d, delayed, tool); }
        @Override public net.minecraft.world.InteractionResult litematica_printer$useItemOn(boolean predict,
                net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
            throw new UnsupportedOperationException();
        }
    }
    static class Fixture implements AutoCloseable {
        final Object oldClient = get(null, Minecraft.class, "instance");
        final Object oldNetworkClient = get(null, NetworkUtils.class, "client");
        final Object oldInteractionClient = get(null, InteractionUtils.class, "client");
        final Object oldRuntime = get(null, RuntimeAccess.class, "current");
        final boolean oldDelayed = Configs.Break.BREAK_USE_DELAYED_DESTROY.getBooleanValue();
        final boolean oldAutoTool = Configs.Break.BREAK_AUTO_TOOL.getBooleanValue();
        final boolean oldWork = Configs.Core.WORK_SWITCH.getBooleanValue();
        final boolean oldIceForWater = Configs.Print.PRINT_ICE_FOR_WATER.getBooleanValue();
        final Object oldMode = Configs.Core.WORK_MODE.getOptionListValue();
        final Object oldType = Configs.Core.WORK_MODE_TYPE.getOptionListValue();
        Minecraft client = alloc(Minecraft.class);
        Player player = alloc(Player.class);
        Level level = alloc(Level.class);
        Connection connection = alloc(Connection.class);
        Port port = new Port();
        MiningInteractionController controller;
        MineHandler mineHandler = alloc(MineHandler.class);
        PrintHandler printHandler = alloc(PrintHandler.class);
        InteractionUtils interactions = new InteractionUtils();
        PrintWorkflowScheduler workflows = new PrintWorkflowScheduler(() -> level.tick, null);
        RuntimeScope scope = new RuntimeScope();
        Fixture(float delta, boolean delayed) throws Exception {
            Configs.Break.BREAK_USE_DELAYED_DESTROY.setBooleanValue(delayed);
            Configs.Break.BREAK_AUTO_TOOL.setBooleanValue(false);
            Configs.Core.WORK_SWITCH.setBooleanValue(true);
            Configs.Core.WORK_MODE.setOptionListValue(WorkingModeType.SINGLE);
            Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.MINE);
            player.speed = delta * 45;
            player.held = ItemStack.EMPTY;
            player.setId(42);
            player.abilities = new Abilities();
            //#if MC >= 260100
            player.inventory = new Inventory(player, new net.minecraft.world.entity.EntityEquipment());
            //#else
            //$$ player.inventory = new Inventory(player);
            //#endif
            set(player, net.minecraft.world.entity.Entity.class, "position", Vec3.ZERO);
            connection.packets = new ArrayList<>();
            set(player, LocalPlayer.class, "connection", connection);
            level.state = Blocks.STONE.defaultBlockState();
            level.border = new WorldBorder();
            level.player = player;
            //#if MC > 11802
            level.predictions = new BlockStatePredictionHandler();
            set(level, ClientLevel.class, "blockStatePredictionHandler", level.predictions);
            //#endif
            level.bridge = new PredictionBridge();
            //#if MC > 11802
            set(level.bridge, MixinClientLevel.class, "blockStatePredictionHandler", level.predictions);
            //#endif
            client.player = player;
            client.level = level;
            var gameMode = alloc(GameMode.class);
            //#if MC == 11802
            //$$ set(gameMode, MultiPlayerGameMode.class, "unAckedActions",
            //$$         new it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap<>());
            //#endif
            gameMode.port = port;
            client.gameMode = gameMode;
            set(client.gameMode, MultiPlayerGameMode.class, "minecraft", client);
            set(client.gameMode, MultiPlayerGameMode.class, "connection", connection);
            set(client.gameMode, MultiPlayerGameMode.class, "localPlayerMode", net.minecraft.world.level.GameType.SURVIVAL);
            set(client, Minecraft.class, "soundManager", alloc(Sounds.class));
            set(client, Minecraft.class, "tutorial", alloc(Tutorial.class));
            set(null, Minecraft.class, "instance", client);
            set(null, NetworkUtils.class, "client", client);
            set(null, InteractionUtils.class, "client", client);
            var tweakeroo = (TweakerooToolSwitchPort) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{TweakerooToolSwitchPort.class}, (p,m,a) -> m.getReturnType() == boolean.class ? false : null);
            port.client = client;
            controller = new MiningInteractionController(port,
                    new ToolSwitchService(client, tweakeroo, new InventorySwitchGuard(client, () -> level.tick)));
            gameMode.controller = controller;
            var ctor = Class.forName("me.aleksilassila.litematica.printer.runtime.MinecraftInteractionRuntime")
                    .getDeclaredConstructor(Minecraft.class);
            ctor.setAccessible(true);
            scope.register((RuntimeComponent) ctor.newInstance(client));
            var runtime = alloc(PrinterRuntime.class);
            var modules = alloc(FeatureModuleSet.class);
            set(modules, FeatureModuleSet.class, "mine", mineHandler);
            set(modules, FeatureModuleSet.class, "print", printHandler);
            set(printHandler, PrintHandler.class, "printTasks", workflows);
            set(runtime, PrinterRuntime.class, "modules", modules);
            set(runtime, PrinterRuntime.class, "interactionUtils", interactions);
            RuntimeAccess.install(runtime);
            for (var name : List.of("mc", "level", "player", "connection", "gameMode")) {
                Object value = switch (name) {
                    case "mc" -> client;
                    case "level" -> level;
                    case "player" -> player;
                    case "connection" -> connection;
                    default -> client.gameMode;
                };
                set(mineHandler, FeatureModuleBase.class, name, value);
                set(printHandler, FeatureModuleBase.class, name, value);
            }
        }
        BlockBreakResult mine() { return controller.continueForMine(TARGET, Direction.DOWN, false); }
        void activeTarget(BlockPos pos) throws Exception { set(mineHandler, MineHandler.class, "activeMinePos", pos); }
        void epoch(String reason) {
            scope.changeEpoch(new RuntimeEvent.EpochChanged(RuntimeEpoch.INITIAL, RuntimeEpoch.INITIAL.next(), reason));
        }
        void switchServer(boolean replaceLevel) throws Exception {
            if (replaceLevel) {
                Level next = alloc(Level.class);
                next.state = Blocks.STONE.defaultBlockState();
                next.border = new WorldBorder();
                next.player = player;
                next.bridge = new PredictionBridge();
                //#if MC > 11802
                next.predictions = new BlockStatePredictionHandler();
                set(next, ClientLevel.class, "blockStatePredictionHandler", next.predictions);
                set(next.bridge, MixinClientLevel.class, "blockStatePredictionHandler", next.predictions);
                //#endif
                client.level = next;
                level = next;
            }
            Connection next = alloc(Connection.class);
            next.packets = new ArrayList<>();
            set(player, LocalPlayer.class, "connection", next);
            connection = next;
            set(client.gameMode, MultiPlayerGameMode.class, "connection", next);
        }
        void idleInput(boolean attack) throws Exception {
            // Before the fix there is no hook, so exercise the original idle cancellation.
            var ci = new CallbackInfo("continueAttack", true);
            try {
                var type = Class.forName("me.aleksilassila.litematica.printer.mixin.MixinMinecraftMiningInput");
                var method = type.getDeclaredMethod("preservePrinterMining", boolean.class, CallbackInfo.class);
                method.setAccessible(true);
                method.invoke(alloc(type), attack, ci);
            } catch (ClassNotFoundException absentBeforeFix) { }
            if (!ci.isCancelled()) vanillaCancel();
        }
        void vanillaCancel() throws Exception {
            copyToVanilla();
            client.gameMode.stopDestroyBlock();
            copyFromVanilla();
        }
        void vanillaStart(BlockPos pos) throws Exception {
            copyToVanilla();
            client.gameMode.startDestroyBlock(pos, Direction.DOWN);
            copyFromVanilla();
        }
        void vanillaContinue(BlockPos pos) throws Exception {
            copyToVanilla();
            client.gameMode.continueDestroyBlock(pos, Direction.DOWN);
            copyFromVanilla();
        }
        void copyToVanilla() throws Exception {
            set(client.gameMode, MultiPlayerGameMode.class, "destroyBlockPos", port.pos);
            set(client.gameMode, MultiPlayerGameMode.class, "destroyProgress", port.progress);
            set(client.gameMode, MultiPlayerGameMode.class, "isDestroying", port.destroying);
            set(client.gameMode, MultiPlayerGameMode.class, "destroyingItem", port.item);
        }
        void copyFromVanilla() throws Exception {
            port.pos = (BlockPos) get(client.gameMode, MultiPlayerGameMode.class, "destroyBlockPos");
            port.progress = (float) get(client.gameMode, MultiPlayerGameMode.class, "destroyProgress");
            port.destroying = (boolean) get(client.gameMode, MultiPlayerGameMode.class, "isDestroying");
            port.item = (ItemStack) get(client.gameMode, MultiPlayerGameMode.class, "destroyingItem");
        }
        WaterPrintTask waterTask(boolean ice) throws Exception {
            Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
            Configs.Print.PRINT_ICE_FOR_WATER.setBooleanValue(true);
            if (ice) {
                level.state = Blocks.ICE.defaultBlockState();
                player.speed = 0.2f * 15;
            }
            var ctor = WaterPrintTask.class.getDeclaredConstructor(BlockPos.class, java.util.function.LongSupplier.class);
            ctor.setAccessible(true);
            WaterPrintTask task = ctor.newInstance(TARGET, (java.util.function.LongSupplier) () -> level.tick);
            @SuppressWarnings("unchecked")
            var tasks = (Map<BlockPos, PrintTask>) get(workflows, PrintWorkflowScheduler.class, "tasks");
            tasks.put(TARGET, task);
            return task;
        }
        void advanceWaterTask(WaterPrintTask task) throws Exception {
            var context = alloc(SchematicBlockContext.class);
            set(context, SchematicBlockContext.class, "client", client);
            set(context, SchematicBlockContext.class, "level", level);
            set(context, SchematicBlockContext.class, "blockPos", TARGET);
            set(context, SchematicBlockContext.class, "currentState", level.state);
            set(context, SchematicBlockContext.class, "requiredState", Blocks.WATER.defaultBlockState());
            task.buildAction(context);
        }
        @Override public void close() throws Exception {
            set(null, Minecraft.class, "instance", oldClient);
            set(null, NetworkUtils.class, "client", oldNetworkClient);
            set(null, InteractionUtils.class, "client", oldInteractionClient);
            set(null, RuntimeAccess.class, "current", oldRuntime);
            Configs.Break.BREAK_USE_DELAYED_DESTROY.setBooleanValue(oldDelayed);
            Configs.Break.BREAK_AUTO_TOOL.setBooleanValue(oldAutoTool);
            Configs.Core.WORK_SWITCH.setBooleanValue(oldWork);
            Configs.Print.PRINT_ICE_FOR_WATER.setBooleanValue(oldIceForWater);
            Configs.Core.WORK_MODE.setOptionListValue((fi.dy.masa.malilib.config.IConfigOptionListEntry) oldMode);
            Configs.Core.WORK_MODE_TYPE.setOptionListValue((fi.dy.masa.malilib.config.IConfigOptionListEntry) oldType);
        }

    }

    @ParameterizedTest @ValueSource(floats = {0.8f, 1.2f})
    void delayedFastBreakRetainsBlockAndWaitsBeforeRetry(float delta) throws Exception {
        try (var f = new Fixture(delta, true)) {
            assertEquals(BlockBreakResult.COMPLETED_WAIT, f.mine());
            assertFalse(f.level.state.isAir());
            assertEquals(0, f.port.localDestroys);
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET));
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK), actions(f));
            for (int tick = 0; tick < 8; tick++) {
                f.level.tick++; f.controller.tick(); f.mine();
                assertEquals(2, f.connection.packets.size(), "Do not resend while confirmation is pending");
            }
            f.level.state = Blocks.AIR.defaultBlockState();
            f.controller.confirmServerBlockUpdate(TARGET);
            assertFalse(f.controller.isPendingDelayedDestroy(TARGET));
            assertEquals(BlockBreakResult.COMPLETED, f.mine());
            assertEquals(2, f.connection.packets.size());
        }
    }

    @Test void delayedCreativeBreakAlsoWaitsWithoutLocalRemoval() throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            f.player.abilities.instabuild = true;
            assertEquals(BlockBreakResult.COMPLETED_WAIT, f.mine());
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET));
            f.mine();
            assertFalse(f.level.state.isAir());
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK), actions(f));
        }
    }

    @Test void longLatencyAndExistingTimeoutRetriesNeverEraseTheUnconfirmedBlock() throws Exception {
        try (var f = new Fixture(1.2f, true)) {
            for (int tick = 0; tick < 400; tick++) {
                f.mine(); f.level.tick++; f.controller.tick();
                assertFalse(f.level.state.isAir(), "Timeout must not act as permission to delete locally");
            }
            assertEquals(0, f.port.localDestroys);
            assertTrue(f.connection.packets.size() > 2, "Existing timeout must still permit retries");
            f.level.state = Blocks.AIR.defaultBlockState();
            f.controller.confirmServerBlockUpdate(TARGET);
            int sent = f.connection.packets.size();
            for (int tick = 0; tick < 20; tick++) { f.level.tick++; f.controller.tick(); f.mine(); }
            assertEquals(sent, f.connection.packets.size());
        }
    }

    @Test void changingDelaySettingCannotBypassAnExistingPendingBreak() throws Exception {
        try (var f = new Fixture(1.2f, true)) {
            f.mine();
            Configs.Break.BREAK_USE_DELAYED_DESTROY.setBooleanValue(false);
            f.mine();
            assertFalse(f.level.state.isAir());
            assertEquals(2, f.connection.packets.size());
        }
    }

    @Test void solidServerReplyDoesNotConfirmADelayedFastBreakOrBlockManualMiningElsewhere() throws Exception {
        try (var f = new Fixture(1.2f, true)) {
            f.mine();
            f.controller.confirmServerBlockUpdate(TARGET);
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET));
            f.player.speed = 0.2f * 45;
            f.vanillaStart(TARGET.east());
            f.vanillaContinue(TARGET.east());
            f.idleInput(false);
            assertFalse(f.port.destroying);
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET));
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK), actions(f));
            assertEquals(0, f.port.localDestroys);
            f.level.state = Blocks.AIR.defaultBlockState();
            f.controller.confirmServerBlockUpdate(TARGET);
            assertFalse(f.controller.isPendingDelayedDestroy(TARGET));
        }
    }

    @Test void explicitVanillaCancellationRestartsWithStartInsteadOfResumingOldProgress() throws Exception {
        try (var f = new Fixture(0.2f, false)) {
            f.mine(); f.level.tick++; f.mine();
            f.vanillaCancel();
            assertEquals(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    actions(f).get(actions(f).size() - 1));
            f.connection.packets.clear();
            assertEquals(BlockBreakResult.IN_PROGRESS, f.mine());
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK), actions(f));
            assertTrue(f.port.destroying);
            assertEquals(0.2f, f.port.progress, 0.0001f);
            assertFalse(f.level.state.isAir());
        }
    }

    @Test void idleInputDoesNotRestartEveryTickAndSlowMiningCanFinish() throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            f.mine(); f.activeTarget(TARGET);
            for (int tick = 0; tick < 5 && !f.controller.isPendingDelayedDestroy(TARGET); tick++) {
                f.level.tick++; f.idleInput(false); f.mine();
            }
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET), "Automatic idle input must not starve slow mining");
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK), actions(f));
            assertFalse(f.level.state.isAir());
        }
    }

    @Test void printRemovalAlsoSurvivesIdleInputAndFinishesSlowMining() throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
            f.interactions.continueDestroyBlock(TARGET);
            for (int tick = 0; tick < 20 && !f.controller.isPendingDelayedDestroy(TARGET); tick++) {
                f.level.tick++; f.idleInput(false);
                f.interactions.continueDestroyBlock(TARGET);
            }
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET),
                    "The shared print removal path must not loop START/ABORT on idle input");
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK), actions(f));
            assertFalse(f.level.state.isAir());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void waterWorkflowUntrackedMiningAlsoSurvivesIdleInput(boolean ice) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            var task = f.waterTask(ice);
            f.advanceWaterTask(task);
            assertFalse(f.interactions.hasActiveDestroyTarget(), "Water workflow owns its own target");
            for (int tick = 0; tick < 20 && !f.controller.isPendingDelayedDestroy(TARGET); tick++) {
                f.level.tick++; f.idleInput(false); f.advanceWaterTask(task);
            }
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET),
                    "Water removal and ice breaking must not restart on every idle tick");
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK), actions(f));
            assertFalse(f.level.state.isAir());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"mine", "print", "disabled"})
    void manualMiningWithoutAnAutomaticOwnerStillStopsOnRelease(String mode) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            Configs.Core.WORK_MODE_TYPE.setOptionListValue(mode.equals("mine") ? PrintModeType.MINE : PrintModeType.PRINTER);
            Configs.Core.WORK_SWITCH.setBooleanValue(!mode.equals("disabled"));
            f.vanillaStart(TARGET);
            f.vanillaContinue(TARGET);
            assertTrue(f.port.destroying);
            assertTrue(f.port.progress > 0);
            f.idleInput(false);
            assertFalse(f.port.destroying);
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK), actions(f));
            assertEquals(0, f.port.localDestroys);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void manualTargetChangeAndReleaseAreNotCapturedByTheOldAutomaticTarget(boolean printing) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            if (printing) {
                Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
                f.interactions.continueDestroyBlock(TARGET);
            } else {
                f.mine(); f.activeTarget(TARGET);
            }
            f.vanillaStart(TARGET.east());
            f.vanillaContinue(TARGET.east());
            f.idleInput(false);
            assertFalse(f.port.destroying);
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK), actions(f));
            assertEquals(TARGET.east(), f.connection.packets.get(3).getPos());
            assertEquals(0, f.port.localDestroys);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void changingTheHeldItemCancelsTheOldSessionBeforeAutomaticRestart(boolean printing) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            if (printing) {
                Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
                f.interactions.continueDestroyBlock(TARGET);
            } else {
                f.mine(); f.activeTarget(TARGET);
            }
            f.player.held = new ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE);
            f.idleInput(false);
            assertFalse(f.port.destroying);
            if (printing) f.interactions.continueDestroyBlock(TARGET);
            else f.mine();
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK), actions(f));
            assertEquals(0.2f, f.port.progress, 0.0001f);
            assertFalse(f.level.state.isAir());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"work", "mode", "queue", "connection", "world", "player", "game_mode"})
    void printIdleProtectionEndsWhenItsOwnerOrClientScopeChanges(String change) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
            f.interactions.continueDestroyBlock(TARGET);
            assertTrue(f.printHandler.shouldKeepMiningOnIdleInput());
            switch (change) {
                case "work" -> Configs.Core.WORK_SWITCH.setBooleanValue(false);
                case "mode" -> Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.MINE);
                case "queue" -> f.interactions.resetRuntime();
                case "connection" -> f.switchServer(false);
                case "world" -> f.switchServer(true);
                case "player" -> f.client.player = null;
                case "game_mode" -> f.client.gameMode = null;
            }
            assertFalse(f.printHandler.shouldKeepMiningOnIdleInput());
            if (List.of("work", "mode", "queue").contains(change)) {
                f.idleInput(false);
                assertFalse(f.port.destroying);
                assertEquals(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        actions(f).get(actions(f).size() - 1));
            }
        }
    }

    @ParameterizedTest @ValueSource(strings = {"removed", "waiting", "disabled"})
    void waterWorkflowOnlyProtectsItsLiveBreakingStage(String change) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            var task = f.waterTask(false);
            f.advanceWaterTask(task);
            assertTrue(f.printHandler.shouldKeepMiningOnIdleInput());
            switch (change) {
                case "removed" -> f.workflows.clear();
                case "waiting" -> set(task, WaterPrintTask.class, "stage", WaterTaskStage.WAIT_REMOVE_CONFIRM);
                case "disabled" -> Configs.Print.PRINT_ICE_FOR_WATER.setBooleanValue(false);
            }
            f.idleInput(false);
            assertFalse(f.port.destroying);
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK), actions(f));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void printCancellationRestartsButDisableAndScopeResetDoNotResend(boolean water) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            var task = water ? f.waterTask(false) : null;
            Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
            if (water) f.advanceWaterTask(task);
            else f.interactions.continueDestroyBlock(TARGET);
            f.vanillaCancel();
            if (water) f.advanceWaterTask(task);
            else f.interactions.continueDestroyBlock(TARGET);
            assertEquals(0.2f, f.port.progress, 0.0001f);
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK), actions(f));
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
            f.idleInput(false);
            f.epoch("work_switch_disabled");
            int sent = f.connection.packets.size();
            for (int tick = 0; tick < 100; tick++) { f.level.tick++; f.controller.tick(); f.idleInput(false); }
            assertEquals(sent, f.connection.packets.size());
            f.switchServer(true);
            f.epoch("client_scope_changed");
            f.idleInput(false);
            assertTrue(f.connection.packets.isEmpty());
            assertEquals(0, f.port.localDestroys);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"work", "mode", "target", "manual", "connection", "world", "vanilla_target"})
    void idleProtectionEndsWhenThePrinterNoLongerOwnsTheOperation(String change) throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            f.mine(); f.activeTarget(TARGET);
            switch (change) {
                case "work" -> Configs.Core.WORK_SWITCH.setBooleanValue(false);
                case "mode" -> Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
                case "target" -> f.activeTarget(null);
                case "connection" -> f.switchServer(false);
                case "world" -> {
                    var connection = f.connection;
                    f.switchServer(true);
                    set(f.player, LocalPlayer.class, "connection", connection);
                    set(f.client.gameMode, MultiPlayerGameMode.class, "connection", connection);
                    f.connection = connection;
                }
                case "vanilla_target" -> f.port.pos = TARGET.east();
            }
            f.idleInput(change.equals("manual"));
            assertFalse(f.port.destroying, "Only the current automatic mining operation may suppress idle cancellation");
            assertEquals(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    actions(f).get(actions(f).size() - 1));
            assertFalse(f.level.state.isAir());
        }
    }

    @Test void disabledPendingFastBreakAcceptsALateReplyWithoutDeletingOrResending() throws Exception {
        try (var f = new Fixture(1.2f, true)) {
            f.mine();
            assertTrue(f.controller.isPendingDelayedDestroy(TARGET));
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
            f.epoch("work_switch_disabled");
            for (int tick = 0; tick < 400; tick++) { f.level.tick++; f.controller.tick(); }
            assertFalse(f.level.state.isAir());
            assertEquals(0, f.port.localDestroys);
            assertFalse(f.controller.isPendingDelayedDestroy(TARGET));
            f.level.state = Blocks.AIR.defaultBlockState();
            f.controller.confirmServerBlockUpdate(TARGET);
            f.controller.tick();
            assertEquals(2, f.connection.packets.size());
        }
    }

    @Test void disabledSlowBreakCancelsAndReenableStartsANewSession() throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            f.mine(); f.activeTarget(TARGET);
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
            f.idleInput(false);
            f.epoch("work_switch_disabled");
            assertFalse(f.port.destroying);
            assertEquals(0, f.port.progress);
            int sent = f.connection.packets.size();
            for (int tick = 0; tick < 50; tick++) { f.level.tick++; f.controller.tick(); }
            assertEquals(sent, f.connection.packets.size());
            Configs.Core.WORK_SWITCH.setBooleanValue(true);
            f.mine();
            assertEquals(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    actions(f).get(actions(f).size() - 1));
            assertTrue(f.port.destroying);
            assertEquals(0.2f, f.port.progress, 0.0001f);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void scopeResetDiscardsOldPendingWorkAndNeverSendsOldActionsOnTheNewConnection(boolean replaceLevel) throws Exception {
        try (var f = new Fixture(1.2f, true)) {
            f.mine();
            var previousLevel = f.level;
            var previousConnection = f.connection;
            f.switchServer(replaceLevel);
            f.epoch("client_scope_changed");
            f.controller.tick();
            assertFalse(f.controller.isPendingDelayedDestroy(TARGET));
            assertTrue(f.connection.packets.isEmpty());
            assertEquals(2, previousConnection.packets.size());
            assertFalse(f.level.state.isAir());
            assertEquals(BlockBreakResult.COMPLETED_WAIT, f.mine());
            assertEquals(2, f.connection.packets.size(), "Same coordinates in the new scope must be independently actionable");
            assertFalse(previousLevel.state.isAir());
        }
    }

    @Test void scopeResetDuringSlowBreakDoesNotSendAbortToTheNewServer() throws Exception {
        try (var f = new Fixture(0.2f, true)) {
            f.mine(); f.activeTarget(TARGET);
            f.switchServer(true);
            f.epoch("client_scope_changed");
            assertFalse(f.port.destroying);
            assertTrue(f.connection.packets.isEmpty());
            f.mine();
            assertEquals(List.of(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK), actions(f));
            assertEquals(0.2f, f.port.progress, 0.0001f);
        }
    }

    //#if MC > 11802
    @Test void disablingPrinterDoesNotDiscardVanillasOutstandingPredictionRollback() throws Exception {
        try (var f = new Fixture(1.2f, false)) {
            f.mine();
            assertTrue(f.level.state.isAir());
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
            f.epoch("work_switch_disabled");
            f.level.predictions.endPredictionsUpTo(2, f.level);
            assertFalse(f.level.state.isAir(), "The ordinary prediction must remain recoverable after printer reset");
            assertEquals(2, f.connection.packets.size());
        }
    }
    //#endif

    static List<ServerboundPlayerActionPacket.Action> actions(Fixture f) {
        return f.connection.packets.stream().map(ServerboundPlayerActionPacket::getAction).toList();
    }
}
//#endif

