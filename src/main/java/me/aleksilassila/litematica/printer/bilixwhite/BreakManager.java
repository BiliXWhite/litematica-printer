package me.aleksilassila.litematica.printer.bilixwhite;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.TweakerooUtils;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.*;
import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.EXCAVATE_WHITELIST;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;

public class BreakManager {
    private static BreakManager INSTANCE = null;
    private static final Set<BlockPos> breakTargets = new HashSet<>();
    private static final Minecraft client = Minecraft.getInstance();
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
    public static void addBlockToBreak(BlockPos pos) {
        breakTargets.add(pos);
    }

    public static boolean hasTargets() {
        return !breakTargets.isEmpty();
    }

    // 是否在破坏列表内
    public static boolean inBreakTargets(BlockPos pos) {
        return breakTargets.contains(pos);
    }

    // 每tick调用一次的方法
    public void onTick() {
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
            updateTarget();
        }

        while ((breakPos = (!breakTargets.isEmpty() && breakPos != null) ? updateTarget() : null) != null) {
            // 检查方块是否已消失或变为流体
            if (!PlaceUtils.canInteracted(breakPos) ||
                    !canBreakBlock(breakPos) ||
                    !breakRestriction(state)
            ) {
                resetBreakTarget();
                continue;
            }

            // 执行挖掘进度更新
            boolean success;
            try {
                if (!ModLoadStatus.isTweakerooLoaded()){
                    if (TweakerooUtils.isToolSwitchEnabled())
                    {
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

    // 提取重复逻辑为私有方法
    private void resetBreakTarget() {
        // 性能优化：避免不必要的remove操作
        if (breakPos != null) {
            Printer.placeCooldownList.put(breakPos, 4);
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

    public static boolean canBreakBlock(BlockPos pos) {
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
        if (EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.TWEAKEROO)) {
            if (!ModLoadStatus.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> equalsBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> equalsBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = EXCAVATE_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return EXCAVATE_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> equalsBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return EXCAVATE_WHITELIST.getStrings().stream()
                        .anyMatch(string -> equalsBlockName(string, blockState));
            } else {
                return true;
            }
        }
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
        ClientLevel world = client.level;
        BlockState currentState = world.getBlockState(pos);
        Block block = currentState.getBlock();

        // 检查方块是否可以破坏，如果可以则执行挖掘操作
        if (canBreakBlock(pos)) {
            if (!ModLoadStatus.isTweakerooLoaded()){
                if (TweakerooUtils.isToolSwitchEnabled())
                {
                    TweakerooUtils.trySwitchToEffectiveTool(pos);
                }
            }
            client.gameMode.continueDestroyBlock(pos, Direction.DOWN);
            client.gameMode.stopDestroyBlock();
            return (world.getBlockState(pos).is(block) && !client.player.isCreative());
        }
        return false;
    }
}
