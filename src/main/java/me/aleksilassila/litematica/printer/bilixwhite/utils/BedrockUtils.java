package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BedrockUtils {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static Method addBlockTaskMethod;
    private static Method clearTaskMethod;
    private static Method isWorkingMethod;
    private static Method setWorkingMethod;
    private static Object taskManagerInstance;

    public static boolean working = false;

    static {
        if (Statistics.loadBedrockMiner) {
            try {
                // 反射 TaskManager
                Class<?> taskManagerClass = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskManager");

                // 获取 getInstance() 静态方法
                Method getInstanceMethod = taskManagerClass.getDeclaredMethod("getInstance");
                taskManagerInstance = getInstanceMethod.invoke(null);

                // 获取实例方法
                addBlockTaskMethod = taskManagerClass.getDeclaredMethod("addBlockTask", ClientWorld.class, BlockPos.class, Block.class);
                clearTaskMethod = taskManagerClass.getDeclaredMethod("clearTask");
                isWorkingMethod = taskManagerClass.getDeclaredMethod("isWorking");
                setWorkingMethod = taskManagerClass.getDeclaredMethod("setWorking", boolean.class);

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
        } catch (IllegalAccessException | InvocationTargetException e) {
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
            return (boolean) isWorkingMethod.invoke(taskManagerInstance);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void setWorking(boolean bool) {
        if (client.player != null && client.player.isCreative() && bool) {
            ZxyUtils.actionBar("创造模式下不支持破基岩！");
            return;
        }

        try {
            setWorkingMethod.invoke(taskManagerInstance, bool);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!bool) clearTask();
        working = bool;
    }

    public static void toggle() {
        if (working) {
            setWorking(false);
        } else {
            setWorking(true);
        }
    }
}
