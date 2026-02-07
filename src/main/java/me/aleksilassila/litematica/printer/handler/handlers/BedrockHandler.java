package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.core.BlockPos;

import java.util.concurrent.atomic.AtomicReference;

public class BedrockHandler extends ClientPlayerTickHandler {
    public BedrockHandler() {
        super("bedrock", PrintModeType.BEDROCK, Configs.Hotkeys.BEDROCK, null, true);
    }

    @Override
    protected int getTickInterval() {
        return Configs.Break.BREAK_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxEffectiveExecutionsPerTick() {
        return Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    protected boolean canExecute() {
        if (player.isCreative()) {
            MessageUtils.setOverlayMessage("创造模式无法使用破基岩模式！");
            return false;
        }
        if (!ModLoadStatus.isBedrockMinerLoaded()) {
            MessageUtils.setOverlayMessage("未安装 Fabric-Bedrock-Miner 模组/游戏版本小于1.19，无法破基岩！");
            return false;
        }
        if (!BedrockUtils.isWorking()) {
            BedrockUtils.setWorking(true);
        }
        if (BedrockUtils.isBedrockMinerFeatureEnable()) {   // 限制原功能(手动点击或使用方块：添加、开关)
            BedrockUtils.setBedrockMinerFeatureEnable(false);
        }
        return true;
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        if (isBlockPosOnCooldown(blockPos)) {
            return;
        }
        BedrockUtils.addToBreakList(blockPos, client.level);
        setBlockPosCooldown(blockPos, 100);
    }
}
