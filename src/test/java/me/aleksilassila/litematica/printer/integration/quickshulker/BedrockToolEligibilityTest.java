//#if MC == 11802 || MC == 12006 || MC == 12101 || MC == 260100 || MC == 260300
package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.handler.handlers.bedrock.BedrockBreaker;
import me.aleksilassila.litematica.printer.handler.handlers.bedrock.BedrockInventory;
import me.aleksilassila.litematica.printer.integration.tweakeroo.TweakerooToolSwitchPort;
import me.aleksilassila.litematica.printer.interaction.ToolSwitchService;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.utils.InventorySwitchGuard;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
//#if MC > 12006
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
//#endif
//#if MC >= 260100
import net.minecraft.world.entity.EntityEquipment;
//#endif
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real admission, tool preparation and executor guard; only world/network boundaries are replaced. */
class BedrockToolEligibilityTest {
    private static final BlockPos PISTON = BlockPos.ZERO.above();
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }

    static <T> T allocate(Class<T> type) throws Exception {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }

    static void set(Object instance, Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    static Object get(Object instance, Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    static class Connection extends ClientPacketListener {
        List<Packet<?>> packets;
        //#if MC >= 12006
        Connection() { super(null, null, null); }
        //#else
        //$$ Connection() { super(null, null, null, null, null); }
        //#endif
        @Override public void send(Packet<?> packet) { packets.add(packet); }
    }

    static class MiningPlayer extends LocalPlayer {
        //#if MC >= 260300
        //$$ MiningPlayer() { super(null, null, null, null, null, null, false, null, null); }
        //#elseif MC >= 260100
        MiningPlayer() { super(null, null, null, null, null, null, false, null); }
        //#else
        //$$ MiningPlayer() { super(null, null, null, null, null, false, false); }
        //#endif
        Inventory inventory;
        Abilities abilities;
        @Override public Inventory getInventory() { return inventory; }
        @Override public Abilities getAbilities() { return abilities; }
        @Override public ItemStack getMainHandItem() { return inventory.getItem(InventoryUtils.getSelectedSlot(inventory)); }
        boolean submerged;
        boolean airborne;
        boolean aquaAffinity;
        double breakSpeed;
        MobEffectInstance haste;
        //#if MC >= 12006
        @Override public boolean onGround() { return !airborne; }
        //#else
        //$$ @Override public boolean isOnGround() { return !airborne; }
        //#endif
        //#if MC >= 260100
        @Override public boolean hasEffect(Holder<MobEffect> effect) { return effect.equals(MobEffects.HASTE) && haste != null; }
        @Override public MobEffectInstance getEffect(Holder<MobEffect> effect) { return effect.equals(MobEffects.HASTE) ? haste : null; }
        //#elseif MC >= 12006
        //$$ @Override public boolean hasEffect(Holder<MobEffect> effect) { return effect.equals(MobEffects.DIG_SPEED) && haste != null; }
        //$$ @Override public MobEffectInstance getEffect(Holder<MobEffect> effect) { return effect.equals(MobEffects.DIG_SPEED) ? haste : null; }
        //#else
        //$$ @Override public boolean hasEffect(MobEffect effect) { return effect == MobEffects.DIG_SPEED && haste != null; }
        //$$ @Override public MobEffectInstance getEffect(MobEffect effect) { return effect == MobEffects.DIG_SPEED ? haste : null; }
        //#endif
        //#if MC >= 12006
        @Override public double getAttributeValue(Holder<Attribute> attribute) { return attribute.equals(Attributes.BLOCK_BREAK_SPEED) ? breakSpeed : 1; }
        @Override public AttributeInstance getAttribute(Holder<Attribute> attribute) {
            var instance = new AttributeInstance(attribute, ignored -> {});
            if (attribute.equals(Attributes.BLOCK_BREAK_SPEED)) instance.setBaseValue(breakSpeed);
            //#if MC > 12006
            if (attribute.equals(Attributes.SUBMERGED_MINING_SPEED) && aquaAffinity) instance.setBaseValue(1);
            //#endif
            return instance;
        }
        //#endif
        //#if MC <= 12006
        //$$ @Override public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        //$$     if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) return getMainHandItem();
        //$$     if (slot != net.minecraft.world.entity.EquipmentSlot.HEAD || !aquaAffinity) return ItemStack.EMPTY;
        //$$     var helmet = new ItemStack(Items.DIAMOND_HELMET);
        //$$     helmet.enchant(Enchantments.AQUA_AFFINITY, 1);
        //$$     return helmet;
        //$$ }
        //#endif
        @Override public boolean isEyeInFluid(TagKey<Fluid> tag) { return submerged && tag.equals(FluidTags.WATER); }
    }

    static class MiningLevel extends ClientLevel implements NetworkUtils.SequenceExtension {
        //#if MC >= 260100
        MiningLevel() { super(null, null, null, null, 0, 0, null, false, 0L, 0); }
        //#else
        //$$ MiningLevel() { super(null, null, null, null, 0, 0, null, null, false, 0L); }
        //#endif
        //#if MC >= 12006
        @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { return net.minecraft.world.flag.FeatureFlags.VANILLA_SET; }
        //#endif
        @Override public BlockState getBlockState(BlockPos pos) {
            return pos.equals(BlockPos.ZERO) ? Blocks.BEDROCK.defaultBlockState()
                    : pos.equals(PISTON) ? Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.EXTENDED, true)
                    : Blocks.REDSTONE_TORCH.defaultBlockState();
        }
    }

    @SuppressWarnings("deprecation")
    static class Fixture implements AutoCloseable {
        final Object oldClient = get(null, Minecraft.class, "instance");
        final Object oldRuntime = get(null, RuntimeAccess.class, "current");
        final Minecraft client = allocate(Minecraft.class);
        final MiningPlayer player = allocate(MiningPlayer.class);
        final MiningLevel level = allocate(MiningLevel.class);
        final Connection connection = allocate(Connection.class);
        final Map<Field, Object> staticClients = new LinkedHashMap<>();
        final List<TagKey<net.minecraft.world.level.block.Block>> oldTags = Blocks.PISTON.builtInRegistryHolder().tags().toList();
        final sun.misc.Unsafe unsafe;
        final InventorySwitchGuard guard;
        long tick;
        Runnable duringPreparation;

        Fixture(int efficiency) throws Exception {
            var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            //#if MC >= 260100
            player.inventory = new Inventory(player, new EntityEquipment());
            //#else
            //$$ player.inventory = new Inventory(player);
            //#endif
            set(player, net.minecraft.world.entity.player.Player.class, "inventory", player.inventory);
            player.abilities = new Abilities();
            player.breakSpeed = 1;
            connection.packets = new ArrayList<>();
            set(player, LocalPlayer.class, "connection", connection);
            client.player = player;
            client.level = level;
            // Initialize cached clients against the previous singleton, then restore them on close.
            for (var type : List.of(BedrockInventory.class, BedrockBreaker.class, InventoryUtils.class,
                    PlayerUtils.class, NetworkUtils.class)) {
                Class.forName(type.getName(), true, type.getClassLoader());
                for (var field : type.getDeclaredFields()) {
                    if (field.getType() != Minecraft.class) continue;
                    field.setAccessible(true);
                    staticClients.put(field, field.get(null));
                    putStatic(field, client);
                }
            }
            set(null, Minecraft.class, "instance", client);
            guard = new InventorySwitchGuard(client, () -> tick);
            var tweakeroo = (TweakerooToolSwitchPort) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{TweakerooToolSwitchPort.class}, (p, m, a) -> {
                        if (m.getName().equals("isDurabilityGuardActive")) return duringPreparation != null;
                        if (m.getName().equals("prepareCurrentTool")) { duringPreparation.run(); return true; }
                        return m.getReturnType() == boolean.class ? false : null;
                    });
            var runtime = allocate(PrinterRuntime.class);
            set(runtime, PrinterRuntime.class, "inventorySwitchGuard", guard);
            set(runtime, PrinterRuntime.class, "toolSwitchService", new ToolSwitchService(client, tweakeroo, guard));
            RuntimeAccess.install(runtime);
            bindTags(List.of(BlockTags.MINEABLE_WITH_PICKAXE));
            player.inventory.setItem(0, tool(efficiency));
            player.inventory.setItem(1, new ItemStack(Items.PISTON, 2));
        }

        ItemStack tool(int efficiency) {
            var tool = new ItemStack(Items.DIAMOND_PICKAXE);
            //#if MC >= 260300
            //$$ tool.enchant(VanillaRegistries.createWorldLookup().lookupOrThrow(Registries.ENCHANTMENT)
            //$$         .getOrThrow(Enchantments.EFFICIENCY), efficiency);
            //#elseif MC > 12006
            tool.enchant(VanillaRegistries.createLookup().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.EFFICIENCY), efficiency);
            //#elseif MC == 12006
            //$$ tool.enchant(Enchantments.EFFICIENCY, efficiency);
            //#else
            //$$ tool.enchant(Enchantments.BLOCK_EFFICIENCY, efficiency);
            //#endif
            return tool;
        }

        @SuppressWarnings("removal")
        void putStatic(Field field, Object value) {
            unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value);
        }

        static void bindTags(Collection<TagKey<net.minecraft.world.level.block.Block>> tags) throws Exception {
            var method = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
            method.setAccessible(true);
            method.invoke(Blocks.PISTON.builtInRegistryHolder(), tags);
        }

        boolean admitted() throws Exception {
            var method = BedrockInventory.class.getDeclaredMethod("canInstantMinePiston", LocalPlayer.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, player);
        }

        boolean ready() throws Exception {
            var method = BedrockBreaker.class.getDeclaredMethod("prepareCriticalTool", BlockPos.class, BlockState.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, PISTON, level.getBlockState(PISTON));
        }

        void sendBreakPackets() throws Exception {
            var method = BedrockBreaker.class.getDeclaredMethod("sendCriticalBreakPackets", BlockPos.class, Direction.class);
            method.setAccessible(true);
            method.invoke(null, PISTON, Direction.DOWN);
        }

        void assertBundleBlocked() throws Exception {
            var type = Class.forName("me.aleksilassila.litematica.printer.handler.handlers.bedrock.BedrockCriticalExecutor");
            var placerType = Class.forName("me.aleksilassila.litematica.printer.handler.handlers.bedrock.BedrockPlacer");
            var ctor = type.getDeclaredConstructor(placerType);
            ctor.setAccessible(true);
            // A rejected bundle must return before ever touching the placer.
            var executor = ctor.newInstance(new Object[]{null});
            var submit = type.getDeclaredMethod("submit", ClientLevel.class, BlockPos.class, BlockPos.class, Direction.class, Set.class);
            submit.setAccessible(true);
            assertFalse((boolean) submit.invoke(executor, level, BlockPos.ZERO, PISTON, Direction.DOWN, Set.of(PISTON.east())));
            assertEquals(0, get(executor, type, "reservedPistons"));
            assertTrue(connection.packets.isEmpty(), "Reject before breaking the torch or piston");
            assertTrue(level.getBlockState(PISTON).getValue(PistonBaseBlock.EXTENDED));
            assertEquals(2, player.inventory.getItem(1).getCount());
        }

        @Override public void close() throws Exception {
            bindTags(oldTags);
            for (var entry : staticClients.entrySet()) putStatic(entry.getKey(), entry.getValue());
            set(null, Minecraft.class, "instance", oldClient);
            set(null, RuntimeAccess.class, "current", oldRuntime);
        }
    }

    @Test void insufficientEfficiencyIsRejectedAtAdmission() throws Exception {
        try (var f = new Fixture(4)) { assertFalse(f.admitted()); }
    }

    @Test void insufficientToolCannotStartCriticalBundle() throws Exception {
        try (var f = new Fixture(4)) { assertFalse(f.ready()); f.assertBundleBlocked(); }
    }

    @Test void submergedWithoutAquaAffinityIsRejectedAtAdmission() throws Exception {
        try (var f = new Fixture(5)) { f.player.submerged = true; assertFalse(f.admitted()); }
    }

    @Test void enteringWaterAfterAdmissionBlocksBundle() throws Exception {
        try (var f = new Fixture(5)) {
            assertTrue(f.admitted());
            f.player.submerged = true;
            assertFalse(f.ready());
            f.assertBundleBlocked();
        }
    }

    @Test void leavingGroundAfterAdmissionBlocksBundleUntilLanding() throws Exception {
        try (var f = new Fixture(5)) {
            assertTrue(f.admitted());
            f.player.airborne = true;
            assertFalse(f.ready());
            f.assertBundleBlocked();
            f.player.airborne = false;
            assertTrue(f.ready());
        }
    }

    @Test void losingHasteAfterAdmissionRequiresRecheckingCurrentProgress() throws Exception {
        try (var f = new Fixture(4)) {
            //#if MC >= 260100
            f.player.haste = new MobEffectInstance(MobEffects.HASTE, 600, 1);
            //#else
            //$$ f.player.haste = new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1);
            //#endif
            assertTrue(f.admitted());
            assertTrue(f.ready());
            f.player.haste = null;
            assertFalse(f.ready());
            f.assertBundleBlocked();
        }
    }

    @Test void dryEfficiencyFivePassesEvenBelowOneFullTickOfProgress() throws Exception {
        try (var f = new Fixture(5)) {
            var progress = PlayerUtils.getDestroyProgress(f.player, Blocks.PISTON.defaultBlockState(), f.player.getMainHandItem());
            assertTrue(progress >= 0.7F && progress < 1.0F);
            assertTrue(f.admitted());
            assertTrue(f.ready());
        }
    }

    @Test void aquaAffinityCanRestoreUnderwaterEligibility() throws Exception {
        try (var f = new Fixture(5)) {
            f.player.submerged = true;
            f.player.aquaAffinity = true;
            assertTrue(f.admitted());
            assertTrue(f.ready());
        }
    }

    @Test void eligibleToolStillWaitsForExistingInventoryBarrier() throws Exception {
        try (var f = new Fixture(5)) {
            f.guard.markSwitchIfNeeded(f.player.getMainHandItem());
            assertFalse(f.ready());
            f.assertBundleBlocked();
            f.tick++;
            assertTrue(f.ready());
            assertTrue(f.connection.packets.isEmpty(), "Preparation alone must not emit break packets");
        }
    }

    @Test void finalHeldToolIsCheckedAfterDurabilityPreparation() throws Exception {
        try (var f = new Fixture(5)) {
            assertTrue(f.admitted());
            var weakTool = f.tool(4);
            // Same item, count and damage: the switch service can report READY, but speed differs.
            f.duringPreparation = () -> f.player.inventory.setItem(0, weakTool);
            assertFalse(f.ready());
            f.assertBundleBlocked();
        }
    }

    @Test void expiredSwitchWaitDoesNotMakeAnInsufficientToolEligible() throws Exception {
        try (var f = new Fixture(4)) {
            f.guard.markSwitchIfNeeded(Items.NETHERITE_PICKAXE);
            f.tick = 4;
            assertFalse(f.ready());
            f.assertBundleBlocked();
            f.tick = 5;
            assertFalse(f.guard.isWaiting());
            assertFalse(f.ready());
            f.assertBundleBlocked();
        }
    }

    @Test void hotbarSelectionPacketPrecedesBothBreakPacketsWithoutWaitingForReply() throws Exception {
        try (var f = new Fixture(5)) {
            f.player.inventory.setItem(2, f.player.inventory.getItem(0));
            f.player.inventory.setItem(0, new ItemStack(Items.STICK));
            assertTrue(f.ready());
            assertEquals(2, InventoryUtils.getSelectedSlot(f.player.inventory));
            f.sendBreakPackets();
            // Capture outbound packets without delivering any server acknowledgement.
            // A delayed connection must still carry selection before START and STOP.
            assertEquals(3, f.connection.packets.size());
            assertEquals(2, assertInstanceOf(ServerboundSetCarriedItemPacket.class, f.connection.packets.get(0)).getSlot());
            assertEquals(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    assertInstanceOf(ServerboundPlayerActionPacket.class, f.connection.packets.get(1)).getAction());
            assertEquals(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    assertInstanceOf(ServerboundPlayerActionPacket.class, f.connection.packets.get(2)).getAction());
        }
    }

    @Test void delayedInventoryCorrectionCannotReuseEarlierEligibility() throws Exception {
        try (var f = new Fixture(5)) {
            assertTrue(f.ready());
            f.tick = 20;
            f.player.inventory.setItem(0, f.tool(4));
            assertFalse(f.ready());
            f.assertBundleBlocked();
            f.player.inventory.setItem(0, f.tool(5));
            assertTrue(f.ready());
        }
    }

    @Test void enteringWaterWhileToolSwitchSettlesIsRecheckedAfterWait() throws Exception {
        try (var f = new Fixture(5)) {
            f.guard.markSwitchIfNeeded(f.player.getMainHandItem());
            assertFalse(f.ready());
            f.player.submerged = true;
            f.tick = 1;
            assertFalse(f.guard.isWaiting());
            assertFalse(f.ready());
            f.assertBundleBlocked();
        }
    }

    //#if MC >= 12006
    @Test void increasedBlockBreakAttributeCanMakeEfficiencyFourEligible() throws Exception {
        try (var f = new Fixture(4)) {
            f.player.breakSpeed = 2;
            //#if MC == 12006
            //$$ assertTrue(f.player.getDestroySpeed(Blocks.PISTON.defaultBlockState()) / 1.5F / 30 >= 0.7F,
            //$$         "The vanilla Player calculation already accepts this tool");
            //#endif
            assertTrue(f.admitted());
            assertTrue(f.ready());
        }
    }

    @Test void loweredBlockBreakAttributeBeforeExecutionBlocksBundle() throws Exception {
        try (var f = new Fixture(5)) {
            assertTrue(f.ready());
            f.player.breakSpeed = 0.5;
            assertFalse(f.ready());
            f.assertBundleBlocked();
        }
    }
    //#endif
}
//#endif
