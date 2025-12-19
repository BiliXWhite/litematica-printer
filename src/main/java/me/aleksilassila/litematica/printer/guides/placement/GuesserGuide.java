package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * This is the placement guide that most blocks will use.
 * It will try to predict the correct player state for producing the right
 * blockState
 * by brute forcing the correct hit vector and look a direction.
 */
public class GuesserGuide extends GeneralPlacementGuide {
    private PrinterPlacementContext contextCache = null;

    protected static Direction[] directionsToTry = new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };
    protected static Vec3[] hitVecsToTry = new Vec3[]{
            new Vec3(-0.25, -0.25, -0.25),
            new Vec3(+0.25, -0.25, -0.25),
            new Vec3(-0.25, +0.25, -0.25),
            new Vec3(-0.25, -0.25, +0.25),
            new Vec3(+0.25, +0.25, -0.25),
            new Vec3(-0.25, +0.25, +0.25),
            new Vec3(+0.25, -0.25, +0.25),
            new Vec3(+0.25, +0.25, +0.25),
    };

    public GuesserGuide(SchematicBlockState state) {
        super(state);
    }

    @Nullable
    @Override
    public PrinterPlacementContext getPlacementContext(LocalPlayer player) {
        if (contextCache != null && !InitHandler.DEBUG_OUTPUT.getBooleanValue())
            return contextCache;

        ItemStack requiredItem = getRequiredItem(player).orElse(ItemStack.EMPTY);
        int slot = getRequiredItemStackSlot(player);

        if (slot == -1)
            return null;

        for (Direction lookDirection : directionsToTry) {
            for (Direction side : directionsToTry) {
                BlockPos neighborPos = state.blockPos.relative(side);
                BlockState neighborState = state.world.getBlockState(neighborPos);
                boolean requiresShift = getRequiresExplicitShift() || isInteractive(neighborState.getBlock());

                if (!canBeClicked(state.world, neighborPos) || // Handle unclickable grass for example
                        BlockUtils.isReplaceable(neighborState))
                    continue;

                Vec3 hitVec = Vec3.atCenterOf(state.blockPos)
                        .add(Vec3.atLowerCornerOf(DirectionUtils.getVector(side)).scale(0.5));

                for (Vec3 hitVecToTry : hitVecsToTry) {
                    Vec3 multiplier = Vec3.atLowerCornerOf(DirectionUtils.getVector(side));
                    multiplier = new Vec3(multiplier.x == 0 ? 1 : 0, multiplier.y == 0 ? 1 : 0,
                            multiplier.z == 0 ? 1 : 0);

                    BlockHitResult hitResult = new BlockHitResult(hitVec.add(hitVecToTry.multiply(multiplier)),
                            side.getOpposite(), neighborPos, false);
                    PrinterPlacementContext context = new PrinterPlacementContext(player, hitResult, requiredItem, slot,
                            lookDirection, requiresShift);
                    BlockState result = getRequiredItemAsBlock(player)
                            .orElse(targetState.getBlock())
                            .getStateForPlacement(context); // FIXME torch shift clicks another torch and getStateForPlacement
                    // is the clicked block, which is true

                    if (result != null
                            && (statesEqual(result, targetState) || correctChestPlacement(targetState, result))) {
                        contextCache = context;
                        return context;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (targetState.getBlock() instanceof SlabBlock)
            return false; // Slabs are a special case

        return super.canExecute(player);
    }

    private boolean correctChestPlacement(BlockState targetState, BlockState result) {
        if (targetState.hasProperty(ChestBlock.TYPE) && result.hasProperty(ChestBlock.TYPE)
                && result.getValue(ChestBlock.FACING) == targetState.getValue(ChestBlock.FACING)) {
            ChestType targetChestType = targetState.getValue(ChestBlock.TYPE);
            ChestType resultChestType = result.getValue(ChestBlock.TYPE);

            return targetChestType != ChestType.SINGLE && resultChestType == ChestType.SINGLE;
        }

        return false;
    }
}
