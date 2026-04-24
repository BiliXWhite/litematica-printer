package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.utils.CooldownUtils;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.mods.ModLoadUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicReference;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;

public class MineHandler extends ClientPlayerTickHandler {
    public final static String NAME = "mine";
    private BlockPos currentBreakPos;
    private boolean skipMainIteration;

    public MineHandler() {
        super(NAME, PrintModeType.MINE, Configs.Core.MINE, Configs.Mine.MINE_SELECTION_TYPE, true);
    }

    public static boolean mineRestriction(BlockState blockState) {
        if (!InteractionUtils.breakRestriction(blockState)) {
            return false;
        }
        if (Configs.Mine.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModLoadUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Mine.EXCAVATE_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Mine.EXCAVATE_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Mine.EXCAVATE_WHITELIST.getStrings().stream()
                        .anyMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        }
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
        if (isBlockPosOnCooldown(pos) || CooldownUtils.INSTANCE.isOnCooldown(level, FluidHandler.NAME, pos)) {
            return false;
        }
        return InteractionUtils.canBreakBlock(pos) && mineRestriction(level.getBlockState(pos));
    }

    @Override
    protected boolean executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        BlockBreakResult result = InteractionUtils.INSTANCE.continueDestroyBlockWithoutTracking(blockPos);
        this.handleBreakResult(blockPos, result);
        if (result == BlockBreakResult.IN_PROGRESS) {
            skipIteration.set(true);
        }
        return result != BlockBreakResult.FAILED;
    }

    @Override
    protected void preprocess() {
        this.skipMainIteration = false;
        if (this.currentBreakPos == null || this.level == null) {
            return;
        }
        if (!InteractionUtils.canBreakBlock(this.currentBreakPos) || !mineRestriction(this.level.getBlockState(this.currentBreakPos))) {
            this.currentBreakPos = null;
            return;
        }
        BlockBreakResult result = InteractionUtils.INSTANCE.continueDestroyBlockWithoutTracking(this.currentBreakPos);
        this.handleBreakResult(this.currentBreakPos, result);
        if (result == BlockBreakResult.IN_PROGRESS) {
            this.skipMainIteration = true;
        }
    }

    @Override
    protected boolean canIterate() {
        return !this.skipMainIteration;
    }

    private void handleBreakResult(BlockPos blockPos, BlockBreakResult result) {
        if (result == BlockBreakResult.IN_PROGRESS) {
            this.currentBreakPos = blockPos;
            return;
        }
        if (this.currentBreakPos != null && this.currentBreakPos.equals(blockPos)) {
            this.currentBreakPos = null;
        }
        if (result == BlockBreakResult.COMPLETED) {
            this.setBlockPosCooldown(blockPos, getBreakCooldown());
        } else if (result == BlockBreakResult.COMPLETED_WAIT) {
            this.setBlockPosCooldown(blockPos, 2);
        }
    }
}
