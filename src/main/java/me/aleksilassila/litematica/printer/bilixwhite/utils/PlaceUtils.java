package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class PlaceUtils {

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


}
