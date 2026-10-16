package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.guide.Result;
import me.aleksilassila.litematica.printer.guide.Guide;
import me.aleksilassila.litematica.printer.guide.DefaultGuide;
import me.aleksilassila.litematica.printer.guide.guides.SkullGuide;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
//#if MC >= 12006
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
//#else
//$$ import net.minecraft.nbt.CompoundTag;
//$$ import net.minecraft.nbt.ListTag;
//#endif
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class InventoryRegressionTest {
    // Headless diagnostics: only allocate the context value object; no Minecraft client is launched.
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        //#if MC >= 260100
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                //#if MC >= 260300
                //$$ .build(net.minecraft.data.registries.VanillaRegistries.createWorldLookup()).forEach(p -> p.apply());
                //#else
                .build(net.minecraft.data.registries.VanillaRegistries.createLookup()).forEach(p -> p.apply());
                //#endif
        //#endif
    }
    static SchematicBlockContext context(BlockState required) throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) f.get(null);
        var ctx = (SchematicBlockContext) unsafe.allocateInstance(SchematicBlockContext.class);
        set(ctx, "blockPos", BlockPos.ZERO);
        set(ctx, "currentState", Blocks.AIR.defaultBlockState());
        set(ctx, "requiredState", required);
        return ctx;
    }
    static void set(Object obj, String field, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(field); f.setAccessible(true); f.set(obj, value);
    }
    static Result missing(Guide guide) throws Exception {
        var method = guide.getClass().getDeclaredMethod("onBuildActionMissingBlock", BlockMatchResult.class);
        method.setAccessible(true);
        return (Result) method.invoke(guide, BlockMatchResult.MISSING);
    }
    @ParameterizedTest @EnumSource(value=Direction.class, names={"NORTH","SOUTH","EAST","WEST"})
    void wallWitherSkullMustProducePlacementAction(Direction facing) throws Exception {
        var state = Blocks.WITHER_SKELETON_WALL_SKULL.defaultBlockState().setValue(WallSkullBlock.FACING, facing);
        assertTrue(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING));
        assertFalse(state.hasProperty(BlockStateProperties.FACING));
        Result result = missing(new SkullGuide(context(state)));
        assertTrue(result.hasAction(), "Wall wither skull skipped: " + facing + ", skipOtherGuide=" + result.skipOtherGuide());
    }
    @ParameterizedTest @ValueSource(ints={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15})
    void standingWitherSkullProducesCorrectMaterialAndYaw(int rotation) throws Exception {
        var state = Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rotation);
        Result result = missing(new SkullGuide(context(state)));
        assertTrue(result.hasAction());
        Action action = result.action();
        assertEquals(Items.WITHER_SKELETON_SKULL, action.getRequiredItems(state.getBlock())[0]);
        assertEquals(rotation, Math.floorMod(Math.round(action.getPlayerLook().getYaw() * 16 / 360), 16));
        assertTrue(action.getSides().containsKey(Direction.DOWN));
    }
    @Test void filledShulkerMustNotBeSelectedForAnEmptySchematicShulker() throws Exception {
        var required = Blocks.SHULKER_BOX.defaultBlockState();
        Action action = missing(new DefaultGuide(context(required))).action();
        ItemStack filled = new ItemStack(Items.SHULKER_BOX);
        //#if MC >= 12006
        filled.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
        //#else
        //$$ CompoundTag stored = new ItemStack(Items.DIAMOND, 64).save(new CompoundTag());
        //$$ stored.putByte("Slot", (byte) 0);
        //$$ ListTag contents = new ListTag();
        //$$ contents.add(stored);
        //$$ filled.getOrCreateTagElement("BlockEntityTag").put("Items", contents);
        //#endif
        boolean accepted = java.util.Arrays.asList(action.getRequiredItems(required.getBlock())).contains(filled.getItem())
                && (action.getRequiredStackPredicate() == null || action.getRequiredStackPredicate().test(filled));
        assertFalse(accepted, "Filled storage shulker passes the generated placement action requirements");
    }
    @Test void disablingOrderlyStorageMustReleaseAnUnopenedPendingRestore() throws Exception {
        var work = me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH;
        var quick = me.aleksilassila.litematica.printer.config.Configs.Placement.QUICK_SHULKER;
        var orderly = me.aleksilassila.litematica.printer.config.Configs.Placement.STORE_ORDERLY;
        boolean oldWork=work.getBooleanValue(), oldQuick=quick.getBooleanValue(), oldOrderly=orderly.getBooleanValue();
        try {
            work.setBooleanValue(true); quick.setBooleanValue(true); orderly.setBooleanValue(false);
            java.util.concurrent.atomic.AtomicBoolean leased = new java.util.concurrent.atomic.AtomicBoolean();
            var portType = me.aleksilassila.litematica.printer.printer.action.ActionPort.class;
            var port = (me.aleksilassila.litematica.printer.printer.action.ActionPort) java.lang.reflect.Proxy.newProxyInstance(
                portType.getClassLoader(), new Class<?>[]{portType}, (proxy,method,args) -> {
                    if (method.getName().equals("tryAcquire")) { leased.set(true); return true; }
                    if (method.getName().equals("releaseOwner")) { leased.set(false); return null; }
                    if (method.getReturnType()==boolean.class) return false;
                    return null;
                });
            var adapter = new me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerAdapter(port);
            var orderedField=adapter.getClass().getDeclaredField("orderedStorage"); orderedField.setAccessible(true);
            var orderedController=orderedField.get(adapter);
            var sessionField=orderedController.getClass().getDeclaredField("restoreSession"); sessionField.setAccessible(true);
            var session=sessionField.get(orderedController);
            // State reachable after a restore container timeout: pending entry retained, waiting=false.
            // No entry fields are read once STORE_ORDERLY is disabled.
            var schedule=session.getClass().getDeclaredMethod("schedule",Object.class); schedule.setAccessible(true);
            schedule.invoke(session,new Object());
            for(int i=0;i<200;i++) adapter.tick();
            assertFalse(adapter.hasPendingRequest() || leased.get(), "Disabled orderly restore still blocks inventory after 200 ticks");
        } finally { work.setBooleanValue(oldWork); quick.setBooleanValue(oldQuick); orderly.setBooleanValue(oldOrderly); }
    }
}
