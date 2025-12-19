package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.actions.PrepareAction;
import me.aleksilassila.litematica.printer.actions.ReleaseShiftAction;
import me.aleksilassila.litematica.printer.guides.Guide;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.implementation.actions.InteractActionImpl;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Guide that clicks its neighbors to create a placement in target position.
 */
abstract public class PlacementGuide extends Guide {
    public PlacementGuide(SchematicBlockState state) {
        super(state);
    }

    protected ItemStack getBlockItem(BlockState state) {
//        return state.getBlock().getPickStack(this.state.world, this.state.blockPos, state);
        return new ItemStack(state.getBlock());
    }

    protected Optional<Block> getRequiredItemAsBlock(LocalPlayer player) {
        Optional<ItemStack> requiredItem = getRequiredItem(player);

        if (requiredItem.isEmpty()) {
            return Optional.empty();
        } else {
            ItemStack itemStack = requiredItem.get();

            if (itemStack.getItem() instanceof BlockItem blockItem)
                return Optional.of(blockItem.getBlock());
            else
                return Optional.empty();
        }
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Collections.singletonList(getBlockItem(state.targetState));
    }

    abstract protected boolean getUseShift(SchematicBlockState state);

    @Nullable
    abstract public PrinterPlacementContext getPlacementContext(LocalPlayer player);

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!super.canExecute(player))
            return false;

        List<ItemStack> requiredItems = getRequiredItems();
        if (requiredItems.isEmpty() || requiredItems.stream().allMatch(i -> i.is(Items.AIR)))
            return false;

        BlockPlaceContext ctx = getPlacementContext(player);
        if (ctx == null || !ctx.canPlace()) return false;
//        if (!state.currentState.getMaterial().isReplaceable()) return false;
        if (!InitHandler.FILL_FLOWING_FLUID.getBooleanValue()
                && getProperty(state.currentState, LiquidBlock.LEVEL).orElse(1) == 0)
            return false;

        BlockState resultState = getRequiredItemAsBlock(player)
                .orElse(targetState.getBlock())
                .getStateForPlacement(ctx);

        if (resultState != null) {
            if (!resultState.canSurvive(state.world, state.blockPos))
                return false;
            return !(currentState.getBlock() instanceof LiquidBlock) || canPlaceInWater(resultState);
        } else {
            return false;
        }
    }

    @Override
    public @NotNull List<Action> execute(LocalPlayer player) {
        List<Action> actions = new ArrayList<>();
        PrinterPlacementContext ctx = getPlacementContext(player);

        if (ctx == null) return actions;
        actions.add(new PrepareAction(ctx));
        actions.add(new InteractActionImpl(ctx));
        if (ctx.shouldSneak) actions.add(new ReleaseShiftAction());

        return actions;
    }

    protected static boolean canBeClicked(Level world, BlockPos pos) {
        return getOutlineShape(world, pos) != Shapes.empty()
                && !(world.getBlockState(pos).getBlock() instanceof SignBlock); // FIXME signs
    }

    private static VoxelShape getOutlineShape(Level world, BlockPos pos) {
        return world.getBlockState(pos).getShape(world, pos);
    }

    @SuppressWarnings("deprecation")
    private boolean canPlaceInWater(BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof LiquidBlockContainer) {
            return true;
        } else if (!(block instanceof DoorBlock) && !(blockState.getBlock() instanceof SignBlock)
                && !blockState.is(Blocks.LADDER) && !blockState.is(Blocks.SUGAR_CANE)
                && !blockState.is(Blocks.BUBBLE_COLUMN)) {
//            Material material = blockState.getMaterial();
//            if (material != Material.PORTAL && material != Material.STRUCTURE_VOID && material != Material.UNDERWATER_PLANT && material != Material.REPLACEABLE_UNDERWATER_PLANT) {
//                return material.blocksMovement();
//            } else {
//                return true;
//            }
            // TODO --> if this ever gets removed 如果这被移除了

            //#if MC > 12000
            return blockState.blocksMotion();
            //#else
            //$$ return blockState.getMaterial().blocksMotion();
            //#endif
        }

        return true;
    }
}
