package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

public class PlaceUtils {
    public static boolean isWaterRequired(BlockState blockState) {
        return
            blockState.isOf(Blocks.WATER) &&
                    blockState.get(FluidBlock.LEVEL) == 0 || (
                blockState.getProperties().contains(Properties.WATERLOGGED) &&
                blockState.get(Properties.WATERLOGGED)
            ) ||
                blockState.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isReplaceable(BlockState state) {
        //#if MC < 11904
        //$$ return state.getMaterial().isReplaceable();
        //#else
        return state.isReplaceable();
        //#endif
    }
}
