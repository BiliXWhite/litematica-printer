package me.aleksilassila.litematica.printer.utils.bedrock;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public interface Miner {
    void addToBreakList(BlockPos pos, ClientLevel world) throws Exception;

    void clearTask() throws Exception;

    boolean isWorking() throws Exception;

    void setWorking(boolean running, boolean showMessage) throws Exception;

    boolean isBedrockMinerFeatureEnable() throws Exception;

    void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) throws Exception;
}
