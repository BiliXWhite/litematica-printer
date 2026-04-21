package me.aleksilassila.litematica.printer.utils.mods.bedrock;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public interface Miner {
    void addToBreakList(BlockPos pos, ClientLevel world) throws Exception;

    void addRegionTask(String name, ClientLevel world, BlockPos pos1, BlockPos pos2) throws Exception;

    void clearTask() throws Exception;

    boolean isWorking() throws Exception;

    void setWorking(boolean running, boolean showMessage) throws Exception;

    boolean isBedrockMinerFeatureEnable() throws Exception;

    void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) throws Exception;

    // 待测试
    boolean isInTasks(ClientLevel world, BlockPos blockPos) throws Exception;
}
