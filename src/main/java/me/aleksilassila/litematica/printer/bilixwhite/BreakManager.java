package me.aleksilassila.litematica.printer.bilixwhite;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.bilixwhite.utils.TweakerooUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.printer.BlockContext;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;

public class BreakManager {
    private static final HashMap<ResourceLocation, BlockPos> breakTargets = new HashMap<>();
    private static final Minecraft client = Minecraft.getInstance();
    private static BreakManager INSTANCE = null;
    private BlockPos breakPos;
    private BlockState state;

    private BreakManager() {
        INSTANCE = this;
    }

    public static @NotNull BreakManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new BreakManager();
        }
        return INSTANCE;
    }

    // 添加需要挖掘的方块
    public static void addBlockToBreak(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        breakTargets.put(level.dimension().location(), pos);
    }

    public static void addBlockToBreak(BlockContext context) {
        if (context == null) return;
        addBlockToBreak(context.level, context.blockPos);
    }

    public static boolean hasTargets() {
        return !breakTargets.isEmpty();
    }

    public static boolean canBreakBlock(BlockPos pos) {
        if (client == null || client.level == null || client.player == null || client.gameMode == null || pos == null) {
            return false;
        }
        ClientLevel world = client.level;
        BlockState currentState = world.getBlockState(pos);
        return !currentState.isAir() &&
                !(currentState.getBlock() instanceof LiquidBlock) &&
                !currentState.is(Blocks.AIR) &&
                !currentState.is(Blocks.CAVE_AIR) &&
                !currentState.is(Blocks.VOID_AIR) &&
                !(currentState.getBlock().defaultDestroyTime() == -1) &&
                !client.player.blockActionRestricted(client.level, pos, client.gameMode.getPlayerMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (Configs.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModLoadStatus.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST
                        .getStrings()
                        .stream()
                        .noneMatch(string -> equalsBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST
                        .getStrings()
                        .stream()
                        .anyMatch(string -> equalsBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.EXCAVATE_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.EXCAVATE_BLACKLIST
                        .getStrings()
                        .stream()
                        .noneMatch(string -> equalsBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.EXCAVATE_WHITELIST
                        .getStrings()
                        .stream()
                        .anyMatch(string -> equalsBlockName(string, blockState));
            } else {
                return true;
            }
        }
    }

    // 每tick调用一次的方法
    public void onTick() {
        if (!(Configs.PRINT_SWITCH.getBooleanValue() || Configs.PRINT.getKeybind().isPressed())) {
            return;
        }
        if (client.player == null || client.level == null || client.gameMode == null) return;

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
            updateTarget(client.level);
        }

        while ((breakPos = (!breakTargets.isEmpty() && breakPos != null) ? updateTarget(client.level) : null) != null) {
            // 检查方块是否已消失或变为流体
            if (!PlayerUtils.canInteracted(breakPos) ||
                    !canBreakBlock(breakPos) ||
                    !breakRestriction(state)
            ) {
                resetBreakTarget(client.level);
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
            } catch (Exception e) {
                // 防止外部方法异常导致 tick 中断
                success = false;
            }
            if (!success) {
                resetBreakTarget(client.level);
            }

            if (!client.player.isCreative() && client.level.getBlockState(breakPos).is(state.getBlock())) {
                return;
            }

        }
    }

    // 提取重复逻辑为私有方法
    private void resetBreakTarget(ClientLevel level) {
        // 性能优化：避免不必要的remove操作
        if (breakPos != null) {
            Printer.getInstance().placeCooldownList.put(breakPos, 4);
            ResourceLocation currentDimension = level.dimension().location();
            breakTargets.entrySet().removeIf(entry ->
                    entry.getKey().equals(currentDimension) && entry.getValue().equals(breakPos));
        }
        updateTarget(level);
        if (breakPos != null) {
            state = level.getBlockState(breakPos);
        } else {
            state = null; // 确保state也被重置
        }
    }

    private BlockPos updateTarget(ClientLevel level) {
        if (level == null || breakTargets.isEmpty()) {
            breakPos = null;
            state = null;
            return null;
        }
        ResourceLocation currentDimension = level.dimension().location();
        Iterator<Map.Entry<ResourceLocation, BlockPos>> iterator = breakTargets.entrySet().iterator();
        while (breakPos == null && iterator.hasNext()) {
            Map.Entry<ResourceLocation, BlockPos> entry = iterator.next();
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                iterator.remove();
                continue;
            }
            // 检查当前世界(维度)与待破坏的方块世界不符, 则删除进行删除
            if (!currentDimension.equals(entry.getKey())) {
                iterator.remove();
                continue;
            }
            breakPos = entry.getValue();
            if (breakPos != null) {
                state = level.getBlockState(breakPos);
            }
        }
        return breakPos;
    }

    // 清空所有待挖掘方块
    public void clear() {
        breakTargets.clear();
        breakPos = null;
        state = null;
    }

    /**
     * 挖掘指定位置的方块
     * 需要每tick都执行一次
     *
     * @param pos 要挖掘的方块位置
     * @return 如果挖掘成功且方块状态未改变则返回true，否则返回false
     */
    public boolean breakBlock(BlockPos pos) {
        // 获取客户端世界对象和方块状态
        if (client == null || client.level == null || client.gameMode == null || client.player == null || pos == null) {
            return false;
        }
        BlockState currentState = client.level.getBlockState(pos);
        Block block = currentState.getBlock();
        // 检查方块是否可以破坏，如果可以则执行挖掘操作
        if (canBreakBlock(pos)) {
            if (ModLoadStatus.isTweakerooLoaded()) {
                if (TweakerooUtils.isToolSwitchEnabled()) {
                    TweakerooUtils.trySwitchToEffectiveTool(pos);
                }
            }
            client.gameMode.continueDestroyBlock(pos, Direction.DOWN);
            return (client.level.getBlockState(pos).is(block) && !client.player.isCreative());
        }
        return false;
    }
}
