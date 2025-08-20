package me.aleksilassila.litematica.printer.bilixwhite;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.EXCAVATE_WHITELIST;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;

public class BreakManager {
    private static BreakManager INSTANCE = null;
    private static final Set<BlockPos> breakTargets = new HashSet<>();
    private static final MinecraftClient client = MinecraftClient.getInstance();
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

    // 是否在破坏列表内
    public static boolean inBreakTargets(BlockPos pos) {
        return breakTargets.contains(pos);
    }

    // 每tick调用一次的方法
    public void onTick() {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

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
            updateBreakTarget();
            if (breakPos != null) {
                state = client.world.getBlockState(breakPos);
            }
        }

        if (breakPos == null) return;

        // 检查方块是否已消失或变为流体
        // 性能优化：重新排列检查顺序，将最可能失败的检查放在前面
        if (!ZxyUtils.canInteracted(breakPos) ||
                !canBreakBlock(breakPos) ||
                !breakRestriction(state) ||
                state.isAir()
        ) {
            resetBreakTarget();
            return;
        }

        // 执行挖掘进度更新
        boolean success;
        try {
            success = client.interactionManager.updateBlockBreakingProgress(breakPos, Direction.DOWN);
        } catch (Exception e) {
            // 防止外部方法异常导致 tick 中断
            success = false;
        }

        // 新增逻辑：即使success为true，也需要检查方块是否已经改变
        if (success && !client.world.getBlockState(breakPos).equals(state)) {
            resetBreakTarget();
            return;
        }

        if (!success) {
            resetBreakTarget();
        }
    }

    // 提取重复逻辑为私有方法
    private void resetBreakTarget() {
        // 性能优化：避免不必要的remove操作
        if (breakPos != null) {
            breakTargets.remove(breakPos);
        }
        updateBreakTarget();
        if (breakPos != null) {
            state = client.world.getBlockState(breakPos);
        } else {
            state = null; // 确保state也被重置
        }
    }

    private void updateBreakTarget() {
        if (breakTargets.isEmpty()) {
            breakPos = null;
            state = null;
            return;
        }
        // 性能优化：使用迭代器直接获取第一个元素，避免创建新的集合
        Iterator<BlockPos> iterator = breakTargets.iterator();
        if (iterator.hasNext()) {
            breakPos = iterator.next();
        } else {
            breakPos = null;
        }
    }

    // 获取待挖掘方块数量
    public int getBlocksToBreakCount() {
        return breakTargets.size();
    }

    // 清空所有待挖掘方块
    public void clear() {
        breakTargets.clear();
        breakPos = null;
        state = null;
    }

    public static boolean canBreakBlock(BlockPos pos) {
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
        return !currentState.isAir() &&
                !(currentState.getBlock() instanceof FluidBlock) &&
                !currentState.isOf(Blocks.AIR) &&
                !currentState.isOf(Blocks.CAVE_AIR) &&
                !currentState.isOf(Blocks.VOID_AIR) &&
                !(currentState.getBlock().getHardness() == -1) &&
                !client.player.isBlockBreakingRestricted(client.world, pos, client.interactionManager.getCurrentGameMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.TWEAKEROO)) {
            if (!FabricLoader.getInstance().isModLoaded("tweakeroo")) return true;
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
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
        Block block = currentState.getBlock();

        // 检查方块是否可以破坏，如果可以则执行挖掘操作
        if (canBreakBlock(pos)) {
            client.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
            client.interactionManager.cancelBlockBreaking();
            return world.getBlockState(pos).isOf(block);
        }
        return false;
    }
}
