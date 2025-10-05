package me.aleksilassila.litematica.printer.bilixwhite.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.state.property.Properties;

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

//    public static int getPerFrameTime() {
//        // 默认最低刷新率30Hz，应该没人会这么比这个还低吧？
//        int refreshRate = Math.max(GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()).refreshRate(), 30);
//        // 返回帧间隔时间（超过这个值了就会卡顿）
//        return Math.max(1, 1000 / refreshRate);
//    }

}
