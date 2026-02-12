package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.mixin_interface.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin_interface.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;

@Environment(EnvType.CLIENT)
public class InteractionUtils {
    public static final Minecraft client = Minecraft.getInstance();
    public static final InteractionUtils INSTANCE = new InteractionUtils();

    private final List<BlockPos> breakTargets = new LinkedList<>();
    private BlockPos breakPos;
    private BlockState state;

    private InteractionUtils() {
    }

    public static boolean canBreakBlock(BlockPos pos) {
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        if (world == null || player == null) return false;
        BlockState currentState = world.getBlockState(pos);
        if (Configs.Break.BREAK_CHECK_HARDNESS.getBooleanValue() && currentState.getBlock().defaultDestroyTime() < 0) {
            return false;
        }
        return !currentState.isAir() &&
                !currentState.is(Blocks.AIR) &&
                !currentState.is(Blocks.CAVE_AIR) &&
                !currentState.is(Blocks.VOID_AIR) &&
                !(currentState.getBlock() instanceof LiquidBlock) &&
                !player.blockActionRestricted(client.level, pos, client.gameMode.getPlayerMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (Configs.Mine.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModLoadStatus.isTweakerooLoaded()) return true;
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

    public void add(BlockPos pos) {
        if (pos == null) return;
        breakTargets.add(pos);
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public void addToFirst(BlockPos pos) {
        if (pos == null) return;
        breakTargets.remove(pos);
        breakTargets.add(0, pos);
    }

    public void addToFirst(SchematicBlockContext ctx) {
        addToFirst(ctx.blockPos);
    }

    public boolean hasTargets() {
        return !breakTargets.isEmpty();
    }

    public boolean inBreakTargets(BlockPos pos) {
        return breakTargets.contains(pos);
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (player == null || level == null || gameMode == null) {
            return;
        }
        // 性能优化：提前检查是否有必要继续执行
        if (breakTargets.isEmpty()) {
            // 确保清理状态
            if (breakPos != null) {
                breakPos = null;
                state = null;
            }
            return;
        }

        // 初始化 breakPos 和 state
        if (breakPos == null) {
            updateTarget();
        }

        while ((breakPos = (!breakTargets.isEmpty() && breakPos != null) ? updateTarget() : null) != null) {
            // 检查方块是否已消失或变为流体
            if (!ConfigUtils.canInteracted(breakPos) || !canBreakBlock(breakPos)) {
                resetBreakTarget();
                continue;
            }
            // 执行挖掘进度更新
            boolean success;
            try {
                if (ModLoadStatus.isTweakerooLoaded()) {
                    if (TweakerooUtils.isToolSwitchEnabled()) {
                        TweakerooUtils.trySwitchToEffectiveTool(breakPos);
                    }
                }
                success = client.gameMode.continueDestroyBlock(breakPos, Direction.DOWN);
                client.gameMode.stopDestroyBlock();
            } catch (Exception e) {
                // 防止外部方法异常导致 tick 中断
                success = false;
            }
            if (!success) {
                resetBreakTarget();
            }

            if (!client.player.isCreative() && client.level.getBlockState(breakPos).is(state.getBlock())) {
                return;
            }

        }
    }

    private void resetBreakTarget() {
        // 性能优化：避免不必要的remove操作
        if (breakPos != null) {
            breakTargets.remove(breakPos);
        }
        updateTarget();
        if (breakPos != null) {
            state = client.level.getBlockState(breakPos);
        } else {
            state = null; // 确保state也被重置
        }
    }

    private BlockPos updateTarget() {
        if (breakTargets.isEmpty()) {
            breakPos = null;
            state = null;
            return null;
        }
        // 性能优化：使用迭代器直接获取第一个元素，避免创建新的集合
        Iterator<BlockPos> iterator = breakTargets.iterator();
        if (iterator.hasNext()) {
            breakPos = iterator.next();
            state = client.level.getBlockState(breakPos);
        } else {
            breakPos = null;
        }
        return breakPos;
    }

    // 清空所有待挖掘方块
    public void clear() {
        breakTargets.clear();
        breakPos = null;
        state = null;
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (blockPos == null || player == null || level == null || gameMode == null) {
            return BlockBreakResult.FAILED;
        }
        MultiPlayerGameModeExtension gameModeExtension = (MultiPlayerGameModeExtension) Reference.MINECRAFT.gameMode;
        if (gameModeExtension != null) {
            return gameModeExtension.litematica_printer$continueDestroyBlock(localPrediction, blockPos, direction);
        }
        return BlockBreakResult.FAILED;
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlock(blockPos, direction, !Configs.Break.BREAK_USE_PACKET.getBooleanValue());
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos) {
        return this.continueDestroyBlock(blockPos, Direction.DOWN);
    }

}