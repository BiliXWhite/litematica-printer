package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.core.BlockPos;

import java.util.concurrent.atomic.AtomicReference;

public class MineHandler extends ClientPlayerTickHandler {
    private BlockPos lastMinedBlock = null;

    public MineHandler() {
        super("mine", PrintModeType.MINE, Configs.Core.MINE, Configs.Mine.MINE_SELECTION_TYPE, true);
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
        if (isBlockPosOnCooldown(pos)) {
            return false;
        }
        return InteractionUtils.canBreakBlock(pos) && InteractionUtils.breakRestriction(level.getBlockState(pos));
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        InteractionUtils.BlockBreakResult result = InteractionUtils.INSTANCE.continueDestroyBlock(blockPos);
        if (result == InteractionUtils.BlockBreakResult.IN_PROGRESS) {
            skipIteration.set(true);
            return;
        }
        this.setBlockPosCooldown(blockPos, getBreakCooldown());
    }

}
