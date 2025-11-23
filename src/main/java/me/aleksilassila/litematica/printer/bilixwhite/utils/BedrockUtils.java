package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import java.lang.reflect.Method;

public class BedrockUtils {
    private static final Minecraft client = Minecraft.getInstance();
    private static Object taskManagerInstance;
    private static Method addBlockTaskMethod;
    private static Method addRegionTaskMethod;
    private static Method clearTaskMethod;
    private static Method isRunningMethod;
    private static Method setRunningMethod;
    private static Method isInTasksMethod;
    private static Method isBedrockMinerFeatureEnableMethod;
    private static Method setBedrockMinerFeatureEnableMethod;

    static {
        if (Statistics.loadBedrockMiner) {
            try {
                Class<?> taskManagerClass = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskManager");
                Method getInstanceMethod = taskManagerClass.getDeclaredMethod("getInstance");
                taskManagerInstance = getInstanceMethod.invoke(null);
                addBlockTaskMethod = taskManagerClass.getDeclaredMethod("addBlockTask", ClientLevel.class, BlockPos.class, Block.class);
                addRegionTaskMethod = taskManagerClass.getDeclaredMethod("addRegionTask", String.class, ClientLevel.class, BlockPos.class, BlockPos.class);
                clearTaskMethod = taskManagerClass.getDeclaredMethod("clearTask");
                isRunningMethod = taskManagerClass.getDeclaredMethod("isRunning");
                setRunningMethod = taskManagerClass.getDeclaredMethod("setRunning", boolean.class, boolean.class);
                isInTasksMethod = taskManagerClass.getDeclaredMethod("isInTasks", ClientLevel.class, BlockPos.class);
                isBedrockMinerFeatureEnableMethod = taskManagerClass.getDeclaredMethod("isBedrockMinerFeatureEnable");
                setBedrockMinerFeatureEnableMethod = taskManagerClass.getDeclaredMethod("setBedrockMinerFeatureEnable", boolean.class);
            } catch (Exception e) {
                e.printStackTrace();
                taskManagerInstance = null;
            }
        }
    }

    public static void addToBreakList(BlockPos pos, ClientLevel world) {
        if (taskManagerInstance == null) return;
        try {
            Block block = world.getBlockState(pos).getBlock();
            addBlockTaskMethod.invoke(taskManagerInstance, world, pos, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addRegionTask(String name, ClientLevel world, BlockPos pos1, BlockPos pos2) {
        if (taskManagerInstance == null) return;
        try {
            addRegionTaskMethod.invoke(taskManagerInstance, name, world, pos1, pos2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearTask() {
        if (taskManagerInstance == null) return;
        try {
            clearTaskMethod.invoke(null); // clearTask 是静态方法，应传递 null
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

    public static void setWorking(boolean running) {
        BedrockUtils.setWorking(running, false);
    }

    public static void setWorking(boolean running, boolean showMessage) {
        if (client.player != null && client.player.isCreative() && running) {
            ZxyUtils.actionBar("创造模式下不支持破基岩！");
            return;
        }
        if (taskManagerInstance == null) return; // 提前检查实例
        try {
            setRunningMethod.invoke(taskManagerInstance, running, showMessage);
            if (!running) clearTask();  // 忘记加了
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isBedrockMinerFeatureEnable() {
        if (taskManagerInstance == null) return false;
        try {
            return (boolean) isBedrockMinerFeatureEnableMethod.invoke(taskManagerInstance);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) {
        if (taskManagerInstance == null) return;
        try {
            setBedrockMinerFeatureEnableMethod.invoke(taskManagerInstance, bedrockMinerFeatureEnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 待测试
    public static boolean isInTasks(ClientLevel world, BlockPos blockPos) { // 修正方法名和参数
        if (taskManagerInstance == null) return false;
        try {
            return (boolean) isInTasksMethod.invoke(taskManagerInstance, world, blockPos);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}