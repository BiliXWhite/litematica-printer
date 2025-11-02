package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class PlaceUtils {
    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();

    /**
     * 判断该方块是否需要水
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterRequired(BlockState blockState) {
        return
            blockState.isOf(Blocks.WATER) &&
                    blockState.get(FluidBlock.LEVEL) == 0 || (
                blockState.getProperties().contains(Properties.WATERLOGGED) &&
                blockState.get(Properties.WATERLOGGED)
            ) ||
                blockState.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.isOf(Blocks.WATER)) return false;
        if (requiredState.isOf(Blocks.WATER) && currentState.get(FluidBlock.LEVEL).equals(requiredState.get(FluidBlock.LEVEL)))
            return true;
        else return currentState.get(FluidBlock.LEVEL) == 0;
    }

    public static boolean isReplaceable(BlockState state) {
        //#if MC < 11904
        //$$ return state.getMaterial().isReplaceable();
        //#else
        return state.isReplaceable();
        //#endif
    }

    public static Direction getFillModeFacing() {
        return switch (LitematicaMixinMod.FILL_BLOCK_FACING.getOptionListValue().getStringValue()) {
            case "down" -> Direction.DOWN;
            case "east" -> Direction.EAST;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "north" -> Direction.NORTH;
            default -> Direction.UP;
        };
    }


    public static boolean canInteracted(BlockPos blockPos) {
        var range = LitematicaMixinMod.PRINTER_RANGE.getIntegerValue();
        return switch (LitematicaMixinMod.ITERATOR_SHAPE.getOptionListValue().getStringValue()) {
            case "sphere" -> canInteractedEuclidean(blockPos, range);
            case "octahedron" -> canInteractedManhattan(blockPos, range);
            default -> true;
        };
    }

    public static boolean canInteractedEuclidean(BlockPos blockPos, double range) {
        var player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= range * range;
    }

    public static boolean canInteractedManhattan(BlockPos pos, int range) {
        BlockPos center = client.player.getBlockPos();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }
}
