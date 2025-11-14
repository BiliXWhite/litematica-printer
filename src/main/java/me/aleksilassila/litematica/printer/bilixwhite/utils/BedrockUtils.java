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
    private static Method addTaskMethod;
    private static Method setWorkingMethod;
    private static Method isWorkingMethod;
    private static Method clearTaskMethod;

    public static boolean working = false;
    static {
        if (Statistics.loadBedrockMiner) {
            try {
                Class<?> taskManager = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskManager");
                addTaskMethod = taskManager.getDeclaredMethod("addTask", Block.class, BlockPos.class, ClientWorld.class);
                setWorkingMethod = taskManager.getDeclaredMethod("setWorking", boolean.class);
                isWorkingMethod = taskManager.getDeclaredMethod("isWorking");
                clearTaskMethod = taskManager.getDeclaredMethod("clearTask");

                //Class<?> taskHandler = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskHandler");

            } catch (Exception ignored) {}
        }
    }

    public static void addToBreakList(BlockPos pos, ClientWorld world) {
        try {
            addTaskMethod.invoke(addTaskMethod, world.getBlockState(pos).getBlock(), pos, world);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearTask() {
        try {
            clearTaskMethod.invoke(clearTaskMethod);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isWorking() {
        try {
            return (boolean) isWorkingMethod.invoke(isWorkingMethod);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setWorking(boolean bool) {
        if (client.player.isCreative() && bool) {
            ZxyUtils.actionBar("创造模式下不支持破基岩！");
        }
        if (!bool) clearTask();
        try {
            setWorkingMethod.invoke(setWorkingMethod, bool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
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
