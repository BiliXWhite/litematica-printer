package me.aleksilassila.litematica.printer.compat;

import me.aleksilassila.litematica.printer.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Compatibility layer for bedrock-miner (mod ID: bedrockminer) and
 * blockminer (mod ID: blockminer).
 * <p>
 * Both mods provide automated breaking of hard blocks (bedrock, etc.)
 * by managing mining tasks in a background task queue. This class
 * detects which mod is installed, resolves the appropriate methods
 * via reflection, and exposes a uniform API.
 * <p>
 * All interactions use reflection. This class compiles and runs safely
 * when neither mod is installed.
 */
public class BedrockCompat {
    private static final Minecraft mc = Minecraft.getInstance();

    private static boolean resolved = false;

    // ── Drilling adapter (whichever mod was detected) ──

    @Nullable private static Object minerInstance;
    @Nullable private static Method addBlockTaskMethod;
    @Nullable private static Method clearTaskMethod;
    @Nullable private static Method isRunningMethod;
    @Nullable private static Method setRunningMethod;
    @Nullable private static Method isFeatureEnableMethod;
    @Nullable private static Method setFeatureEnableMethod;

    private static boolean isResolved() {
        if (!resolved) resolve();
        return minerInstance != null;
    }

    /**
     * Detect the available miner mod and resolve reflection handles.
     */
    private static void resolve() {
        if (resolved) return;
        resolved = true;

        if (ModUtils.isBlockMinerLoaded()) {
            resolveBlockMiner();
        } else if (ModUtils.isBedrockMinerLoaded()) {
            resolveBedrockMiner();
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void resolveBlockMiner() {
        try {
            Class<?> modClass = Class.forName("me.z7087.blockminer.BlockMinerMod");
            Method getInstance = modClass.getDeclaredMethod("getInstance");
            Object modContainer = getInstance.invoke(null);
            Method getTaskManager = modContainer.getClass().getDeclaredMethod("getTaskManager");
            minerInstance = getTaskManager.invoke(modContainer);

            Class<?> tmClass = Class.forName("me.z7087.blockminer.task.TaskManager");
            addBlockTaskMethod = tmClass.getDeclaredMethod("handleAttackBlock", BlockPos.class);
            clearTaskMethod    = tmClass.getDeclaredMethod("clearTasks");
            clearTaskMethod.setAccessible(true);
            isRunningMethod    = tmClass.getDeclaredMethod("isEnabled");
            setRunningMethod   = null; // handled via two separate methods
            Method enable      = tmClass.getDeclaredMethod("onEnable");
            Method disable     = tmClass.getDeclaredMethod("onDisable");
            enable.setAccessible(true);
            disable.setAccessible(true);
            // Store enable/disable in unused slots as a pair
            setRunningMethod   = null; // we handle setRunning via onEnable/onDisable
            // Since setRunning isn't a simple toggle, we handle it locally
            isFeatureEnableMethod  = null; // blockminer doesn't have this feature toggle
            setFeatureEnableMethod = null;
        } catch (Exception ignored) {
            clear();
        }
    }

    private static void resolveBedrockMiner() {
        try {
            Class<?> tmClass = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskManager");
            Method getInstance = tmClass.getDeclaredMethod("getInstance");
            minerInstance = getInstance.invoke(null);

            addBlockTaskMethod     = tmClass.getDeclaredMethod("addBlockTask", ClientLevel.class, BlockPos.class, Block.class);
            clearTaskMethod        = tmClass.getDeclaredMethod("clearTask");
            isRunningMethod        = tmClass.getDeclaredMethod("isRunning");
            setRunningMethod       = tmClass.getDeclaredMethod("setRunning", boolean.class, boolean.class);
            isFeatureEnableMethod  = tmClass.getDeclaredMethod("isBedrockMinerFeatureEnable");
            setFeatureEnableMethod = tmClass.getDeclaredMethod("setBedrockMinerFeatureEnable", boolean.class);
        } catch (Exception ignored) {
            clear();
        }
    }

    private static void clear() {
        minerInstance = null;
        addBlockTaskMethod = null;
        clearTaskMethod = null;
        isRunningMethod = null;
        setRunningMethod = null;
        isFeatureEnableMethod = null;
        setFeatureEnableMethod = null;
    }

    // ================================================================
    //  Public API
    // ================================================================

    /** Whether either miner mod is loaded and its API was resolved. */
    public static boolean isAvailable() {
        return isResolved();
    }

    /** Add a block position to the miner's break queue. */
    public static void addToBreakList(BlockPos pos, ClientLevel world) {
        if (!isResolved()) return;
        try {
            if (ModUtils.isBlockMinerLoaded()) {
                addBlockTaskMethod.invoke(minerInstance, pos);
            } else {
                Block block = world.getBlockState(pos).getBlock();
                addBlockTaskMethod.invoke(minerInstance, world, pos, block);
            }
        } catch (Exception ignored) {}
    }

    /** Clear all pending break tasks. */
    public static void clearTasks() {
        if (!isResolved()) return;
        try {
            clearTaskMethod.invoke(minerInstance);
        } catch (Exception ignored) {}
    }

    /** Check whether the miner is currently running. */
    public static boolean isWorking() {
        if (!isResolved()) return false;
        try {
            return (boolean) isRunningMethod.invoke(minerInstance);
        } catch (Exception e) {
            return false;
        }
    }

    /** Start or stop the miner. */
    public static void setWorking(boolean running) {
        setWorking(running, false);
    }

    /** Start or stop the miner, with optional message. */
    public static void setWorking(boolean running, boolean showMessage) {
        if (!isResolved()) return;
        try {
            if (ModUtils.isBlockMinerLoaded()) {
                Class<?> tmClass = Class.forName("me.z7087.blockminer.task.TaskManager");
                if (running) {
                    Method enable = tmClass.getDeclaredMethod("onEnable");
                    enable.setAccessible(true);
                    enable.invoke(minerInstance);
                } else {
                    Method disable = tmClass.getDeclaredMethod("onDisable");
                    disable.setAccessible(true);
                    disable.invoke(minerInstance);
                }
            } else {
                setRunningMethod.invoke(minerInstance, running, showMessage);
                if (!running) clearTasks();
            }
        } catch (Exception ignored) {}
    }

    /** BedrockMiner-specific: check whether its feature toggle is enabled. */
    public static boolean isFeatureEnable() {
        if (!isResolved() || isFeatureEnableMethod == null) return true;
        try {
            return (boolean) isFeatureEnableMethod.invoke(minerInstance);
        } catch (Exception e) {
            return true;
        }
    }

    /** BedrockMiner-specific: toggle its feature switch. */
    public static void setFeatureEnable(boolean enabled) {
        if (!isResolved() || setFeatureEnableMethod == null) return;
        try {
            setFeatureEnableMethod.invoke(minerInstance, enabled);
        } catch (Exception ignored) {}
    }
}
