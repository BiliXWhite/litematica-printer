package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.printer.BlockPosCooldownManager;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import me.aleksilassila.litematica.printer.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicReference;

public class MineHandler extends ClientPlayerTickHandler {
    public final static String NAME = "mine";

    public MineHandler() {
        super(NAME, PrintModeType.MINE, Configs.Core.MINE, Configs.Mine.MINE_SELECTION_TYPE, true);
    }

    public static boolean mineRestriction(BlockState blockState) {
        if (!LitematicaUtils.breakRestriction(blockState)) {
            return false;
        }
        if (Configs.Mine.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> LitematicaUtils.matchBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> LitematicaUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Mine.EXCAVATE_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Mine.EXCAVATE_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> LitematicaUtils.matchBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Mine.EXCAVATE_WHITELIST.getStrings().stream()
                        .anyMatch(string -> LitematicaUtils.matchBlockName(string, blockState));
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
    protected int getMaxExecutions() {
        return Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    public boolean canProcessPos(BlockPos pos) {
        if (isOnCooldown(pos) || BlockPosCooldownManager.INSTANCE.isOnCooldown(level, FluidHandler.NAME, pos)) {
            return false;
        }
        return LitematicaUtils.canBreakBlock(pos) && mineRestriction(level.getBlockState(pos));
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        Block block = level.getBlockState(blockPos).getBlock();

        // 继续挖掘（规律是每tick发一次包）
        BlockBreakResult result = LitematicaUtils.INSTANCE.continueDestroyBlock(blockPos);

        if (Configs.Break.CHECK_BY_BLOCK_CHANGE.getBooleanValue()) {
            if (level.getBlockState(blockPos).is(block)) {
                skipIteration.set(true);
                return;
            }
        } else if (result == BlockBreakResult.IN_PROGRESS) {
            skipIteration.set(true);    // 本 TICK 退出剩下位置迭代
            this.setCooldown(blockPos, getBreakCooldown());
            return;
        }
        this.setCooldown(blockPos, getBreakCooldown());
    }

}
