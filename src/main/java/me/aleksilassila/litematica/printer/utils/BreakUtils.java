package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.MiningFilterType;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
public class BreakUtils {
    public static final Minecraft client = Minecraft.getInstance();
    public static final BreakUtils INSTANCE = new BreakUtils();

    private final Queue<BlockPos> breakQueue = new LinkedList<>();
    private BlockPos breakPos;

    /**
     * 记录最近完成破坏的方块位置及其剩余冷却刻数。
     * 用于破冰放水等场景：冰破坏后客户端预测为空气，需等待服务端方块更新到达。
     * 冷却期间打印处理器应跳过该位置。
     */
    private final Map<BlockPos, Integer> recentlyBroken = new HashMap<>();

    private BreakUtils() {
    }

    // Add methods from LitematicaUtils
    public static boolean canBreakBlock(BlockPos pos) {
        ClientLevel world = LitematicaUtils.client.level;
        LocalPlayer player = LitematicaUtils.client.player;
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
                !player.blockActionRestricted(LitematicaUtils.client.level, pos, LitematicaUtils.client.gameMode.getPlayerMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (Configs.Break.BREAK_LIMITER.getOptionListValue().equals(MiningFilterType.TWEAKEROO)) {
            if (!ModUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Break.BREAK_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Break.BREAK_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Break.BREAK_WHITELIST.getStrings().stream()
                        .anyMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        }
    }

    public void add(BlockPos pos) {
        if (pos == null) return;
        breakQueue.add(pos);
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public boolean inQueue(BlockPos pos) {
        return breakQueue.contains(pos);
    }

    public boolean inQueue(SchematicBlockContext ctx) {
        return inQueue(ctx.blockPos);
    }

    /**
     * 每tick递减recentlyBroken冷却，移除到期项。需在preprocess前调用。
     */
    private void tickRecentlyBroken() {
        if (recentlyBroken.isEmpty()) return;
        Iterator<Map.Entry<BlockPos, Integer>> it = recentlyBroken.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    public void preprocess() {
        tickRecentlyBroken();
        if (!ConfigUtils.isPrinterEnable()) {
            if (!breakQueue.isEmpty()) {
                breakQueue.clear();
            }
            if (breakPos != null) {
                breakPos = null;
            }
            if (!recentlyBroken.isEmpty()) {
                recentlyBroken.clear();
            }
        }
    }

    public boolean isNeedHandle() {
        return !breakQueue.isEmpty() || breakPos != null;
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }
        if (breakPos == null && breakQueue.isEmpty()) {
            return;
        }
        if (breakPos == null) {
            while (!breakQueue.isEmpty()) {
                BlockPos pos = breakQueue.poll();
                if (pos == null) {
                    continue;
                }
                if (!PlayerUtils.canInteracted(pos) || !canBreakBlock(pos) || !breakRestriction(level.getBlockState(pos))) {
                    continue;
                }
                if (ModUtils.isTweakerooLoaded()) {
                    if (ModUtils.isToolSwitchEnabled()) {
                        ModUtils.trySwitchToEffectiveTool(pos);
                    }
                }
                if (continueDestroyBlock(pos, Direction.DOWN) == BlockBreakResult.IN_PROGRESS) {
                    breakPos = pos;
                    break;
                }
            }
        } else if (continueDestroyBlock(breakPos, Direction.DOWN) != BlockBreakResult.IN_PROGRESS) {
            // 破坏完成：记录该位置到最近破坏冷却映射中，延迟1tick
            // 防止同一tick内打印处理器读到客户端预测的空气状态而错误地重新放置方块
            BlockPos completedPos = breakPos;
            breakPos = null;
            recentlyBroken.put(completedPos, 1);
            onTick();
        }
    }

    /**
     * 检查指定位置是否在最近破坏冷却中
     */
    public boolean isRecentlyBroken(BlockPos pos) {
        return pos != null && recentlyBroken.containsKey(pos);
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        BlockBreakResult result = gameMode.litematica_printer$continueDestroyBlock(localPrediction, blockPos, direction);
        if (result == BlockBreakResult.IN_PROGRESS) {
            breakPos = blockPos;
        }
        return result;
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlock(blockPos, direction, !Configs.Break.BREAK_USE_PACKET.getBooleanValue());
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos) {
        return this.continueDestroyBlock(blockPos, Direction.DOWN);
    }
}