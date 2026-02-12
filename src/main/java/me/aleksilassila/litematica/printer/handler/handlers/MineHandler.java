package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.printer.BlockPosCooldownManager;
import me.aleksilassila.litematica.printer.mixin_interface.BlockBreakResult;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.core.BlockPos;

import java.util.concurrent.atomic.AtomicReference;

public class MineHandler extends ClientPlayerTickHandler {
    public final static String NAME = "mine";

    public MineHandler() {
        super(NAME, PrintModeType.MINE, Configs.Core.MINE, Configs.Mine.MINE_SELECTION_TYPE, true);
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
    public boolean canIterationBlockPos(BlockPos pos) {
        if (isBlockPosOnCooldown(pos) || BlockPosCooldownManager.INSTANCE.isOnCooldown(level, FluidHandler.NAME, pos)) {
            return false;
        }
        return InteractionUtils.canBreakBlock(pos) && InteractionUtils.breakRestriction(level.getBlockState(pos));
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        BlockBreakResult result = InteractionUtils.INSTANCE.continueDestroyBlock(blockPos);
        if (result == BlockBreakResult.IN_PROGRESS) {
            skipIteration.set(true);    // 本 TICK 退出剩下位置迭代
            InteractionUtils.INSTANCE.addToFirst(blockPos); // 添加到破坏队列中去
            this.setBlockPosCooldown(blockPos, getBreakCooldown());
            return;
        }
        this.setBlockPosCooldown(blockPos, getBreakCooldown());
    }

}
