package me.aleksilassila.litematica.printer.utils.mods.bedrock;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Method;

public class BlockMiner implements Miner {
    public final Object instance;

    private final Method addBlockTaskMethod;
    private final Method addRegionTaskMethod;
    private final Method clearTaskMethod;
    private final Method isRunningMethod;
    private final Method setEnableMethod;
    private final Method setDisableMethod;

    private final Method isInTasksMethod;

    public BlockMiner() throws Exception {
        Class<?> taskManagerClass = Class.forName("me.z7087.blockminer.task.TaskManager");
        Method getInstanceMethod = Class.forName("me.z7087.blockminer.BlockMinerMod").getDeclaredMethod("getInstance");
        Object modContainer = getInstanceMethod.invoke(null);
        Method getTaskManagerMethod = modContainer.getClass().getDeclaredMethod("getTaskManager");
        instance = getTaskManagerMethod.invoke(modContainer);
        addBlockTaskMethod = taskManagerClass.getDeclaredMethod("handleAttackBlock", BlockPos.class);
        addRegionTaskMethod = taskManagerClass.getDeclaredMethod("addAura", BlockPos.class, BlockPos.class);
        clearTaskMethod = taskManagerClass.getDeclaredMethod("clearTasks");
        clearTaskMethod.setAccessible(true);
        isRunningMethod = taskManagerClass.getDeclaredMethod("isEnabled");
        setEnableMethod = taskManagerClass.getDeclaredMethod("onEnable");
        setEnableMethod.setAccessible(true);
        setDisableMethod = taskManagerClass.getDeclaredMethod("onDisable");
        setDisableMethod.setAccessible(true);
        isInTasksMethod = taskManagerClass.getDeclaredMethod("isTaskExists", BlockPos.class);
    }

    @Override
    public void addToBreakList(BlockPos pos, ClientLevel world) throws Exception {
        if (this.instance == null) return;
        this.addBlockTaskMethod.invoke(this.instance, pos);
    }

    @Override
    public void addRegionTask(String name, ClientLevel world, BlockPos pos1, BlockPos pos2) throws Exception {
        if (this.instance == null) return;
        this.addRegionTaskMethod.invoke(this.instance, pos1, pos2);
    }
    @Override
    public void clearTask() throws Exception {
        if (this.instance == null) return;
        this.clearTaskMethod.invoke(this.instance);
    }

    @Override
    public boolean isWorking() throws Exception {
        if (this.instance == null) return false;
        return (boolean) this.isRunningMethod.invoke(this.instance);
    }

    @Override
    public void setWorking(boolean running, boolean showMessage) throws Exception {
        if (this.instance == null) return; // 提前检查实例
        if (running) {
            this.setEnableMethod.invoke(this.instance);
        } else {
            this.setDisableMethod.invoke(this.instance);
        }
    }

    // blockminer 没有这种开关
    @Override
    public boolean isBedrockMinerFeatureEnable() {
        return true;
    }

    @Override
    public void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) {
    }

    @Override
    public boolean isInTasks(ClientLevel world, BlockPos blockPos) throws Exception { // 修正方法名和参数
        if (this.instance == null) return false;
        return (boolean) this.isInTasksMethod.invoke(this.instance, blockPos);
    }
}
