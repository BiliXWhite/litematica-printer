package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.state.property.Properties;
import org.lwjgl.glfw.GLFW;

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

    public static int getPerFrameTime() {
        // 默认最低刷新率30Hz，应该没人会这么比这个还低吧？
        int refreshRate = Math.max(GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()).refreshRate(), 30);
        // 返回帧间隔时间（超过这个值了就会卡顿）
        return Math.max(1, 1000 / refreshRate);
    }

    public static boolean isFrameTimeOut() {
        if (!LitematicaMixinMod.FRAME_TIMEOUT.getBooleanValue()) return false;
        var maxFrameTime = Printer.getPrinter().tickStartTime + getPerFrameTime() + LitematicaMixinMod.FRAME_EXTRA_TIME.getIntegerValue();
        return System.currentTimeMillis() > maxFrameTime;
    }
}
