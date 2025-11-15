package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public class BedrockUtils {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static Method addBlockTaskMethod;
    private static Method addRegionTaskMethod;
    private static Method clearTaskMethod;
    private static Method isRunningMethod;
    private static Method setRunningMethod;
    private static Object taskManagerInstance;

    static {
        if (Statistics.loadBedrockMiner) {
            try {
                Class<?> taskManagerClass = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskManager");
                Method getInstanceMethod = taskManagerClass.getDeclaredMethod("getInstance");
                taskManagerInstance = getInstanceMethod.invoke(null);
                addBlockTaskMethod = taskManagerClass.getDeclaredMethod("addBlockTask", ClientWorld.class, BlockPos.class, Block.class);
                addRegionTaskMethod = taskManagerClass.getDeclaredMethod("addRegionTask", String.class, ClientWorld.class, BlockPos.class, BlockPos.class);
                clearTaskMethod = taskManagerClass.getDeclaredMethod("clearTask");
                isRunningMethod = taskManagerClass.getDeclaredMethod("isRunning");
                setRunningMethod = taskManagerClass.getDeclaredMethod("setRunning", boolean.class);

            } catch (Exception e) {
                e.printStackTrace();
                taskManagerInstance = null;
            }
        }
    }

    public static void addToBreakList(BlockPos pos, ClientWorld world) {
        if (taskManagerInstance == null) return;
        try {
            Block block = world.getBlockState(pos).getBlock();
            addBlockTaskMethod.invoke(taskManagerInstance, world, pos, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addRegionTask(String name, ClientWorld world, BlockPos pos1, BlockPos pos2) {
        if (taskManagerInstance == null) return;
        try {
            addRegionTaskMethod.invoke(taskManagerInstance, world, pos1, pos2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearTask() {
        if (taskManagerInstance == null) return;
        try {
            clearTaskMethod.invoke(taskManagerInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isWorking() {
        if (taskManagerInstance == null) return false;
        try {
            return (boolean) isRunningMethod.invoke(taskManagerInstance);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setWorking(boolean bool) {
        if (client.player != null && client.player.isCreative() && bool) {
            ZxyUtils.actionBar("创造模式下不支持破基岩！");
            return;
        }
        try {
            setRunningMethod.invoke(taskManagerInstance, bool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!bool) clearTask();
    }
}