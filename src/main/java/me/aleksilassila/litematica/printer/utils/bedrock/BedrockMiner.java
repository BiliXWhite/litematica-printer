package me.aleksilassila.litematica.printer.utils.bedrock;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Method;

public class BedrockMiner implements Miner {
    public final Object instance;

    private final Method addBlockTaskMethod;
    private final Method clearTaskMethod;
    private final Method isRunningMethod;
    private final Method setRunningMethod;
    private final Method isBedrockMinerFeatureEnableMethod;
    private final Method setBedrockMinerFeatureEnableMethod;

    public BedrockMiner() throws Exception {
        Class<?> taskManagerClass = Class.forName("com.github.bunnyi116.bedrockminer.task.TaskManager");
        Method getInstanceMethod = taskManagerClass.getDeclaredMethod("getInstance");
        this.instance = getInstanceMethod.invoke(null);
        this.addBlockTaskMethod = taskManagerClass.getDeclaredMethod("addBlockTask", ClientLevel.class, BlockPos.class, Block.class);
        this.clearTaskMethod = taskManagerClass.getDeclaredMethod("clearTask");
        this.isRunningMethod = taskManagerClass.getDeclaredMethod("isRunning");
        this.setRunningMethod = taskManagerClass.getDeclaredMethod("setRunning", boolean.class, boolean.class);
        this.isBedrockMinerFeatureEnableMethod = taskManagerClass.getDeclaredMethod("isBedrockMinerFeatureEnable");
        this.setBedrockMinerFeatureEnableMethod = taskManagerClass.getDeclaredMethod("setBedrockMinerFeatureEnable", boolean.class);
    }

    // Add methods from BlockUtils
    @Override
    public void addToBreakList(BlockPos pos, ClientLevel world) throws Exception {
        if (this.instance == null) return;
        Block block = world.getBlockState(pos).getBlock();
        this.addBlockTaskMethod.invoke(this.instance, world, pos, block);
    }

    @Override
    public void clearTask() throws Exception {
        if (this.instance == null) return;
        this.clearTaskMethod.invoke(null); // clearTask 是静态方法，应传递 null
    }

    @Override
    public boolean isWorking() throws Exception {
        if (this.instance == null) return false;
        return (boolean) this.isRunningMethod.invoke(this.instance);
    }

    @Override
    public void setWorking(boolean running, boolean showMessage) throws Exception {
        if (this.instance == null) return; // 提前检查实例
        this.setRunningMethod.invoke(this.instance, running, showMessage);
    }

    @Override
    public boolean isBedrockMinerFeatureEnable() throws Exception {
        if (this.instance == null) return false;
        return (boolean) this.isBedrockMinerFeatureEnableMethod.invoke(this.instance);
    }

    @Override
    public void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) throws Exception {
        if (this.instance == null) return;
        this.setBedrockMinerFeatureEnableMethod.invoke(this.instance, bedrockMinerFeatureEnable);
    }
}
