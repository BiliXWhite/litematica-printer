package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.MiningFilterType;
import me.aleksilassila.litematica.printer.mixin.extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin.extension.MultiPlayerGameModeExtension;
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
import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
public class BreakUtils {
    private static final Minecraft client = Minecraft.getInstance();
    public static final BreakUtils INSTANCE = new BreakUtils();

    private final Queue<BlockPos> breakQueue = new LinkedList<>();
    private final Set<BlockPos> breakSet = new HashSet<>(); // O(1) 查询伴侣
    private BlockPos breakPos;
    @Nullable
    private BooleanSupplier selectionValidity;

    private BreakUtils() {}

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
                        .noneMatch(string -> MatchUtils.matches(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> MatchUtils.matches(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Break.BREAK_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Break.BREAK_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> MatchUtils.matches(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Break.BREAK_WHITELIST.getStrings().stream()
                        .anyMatch(string -> MatchUtils.matches(string, blockState));
            } else {
                return true;
            }
        }
    }

    public void add(BlockPos pos) {
        if (pos == null) return;
        breakQueue.add(pos);
        breakSet.add(pos);
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public boolean inQueue(BlockPos pos) {
        return breakSet.contains(pos);
    }

    public boolean isBreaking(BlockPos pos) {
        return pos != null && pos.equals(breakPos);
    }

    /** 由模块提供：当前挖掘目标是否仍属于模块的有效选区。 */
    public void setSelectionValidity(@Nullable BooleanSupplier validity) {
        this.selectionValidity = validity;
    }

    private boolean canWorkAt(BlockPos pos) {
        if (pos == null || !PlayerUtils.canInteracted(pos)) return false;
        // 渲染层是扫描框的全局限制，切换层后必须立即丢弃旧目标。
        if (!LitematicaUtils.isPositionWithinRange(pos)) return false;
        BooleanSupplier validity = this.selectionValidity;
        return validity == null || validity.getAsBoolean();
    }

    private void stopBreaking() {
        BlockPos pos = breakPos;
        // 先释放目标，让 keepPrinterMining 不再拦截原版的取消挖掘和裂纹清理。
        breakPos = null;
        this.selectionValidity = null;
        if (pos == null || client.player == null || client.level == null || client.gameMode == null) return;
        MultiPlayerGameModeExtension gameMode = (MultiPlayerGameModeExtension) client.gameMode;
        if (gameMode.litematica_printer$isDestroying() && pos.equals(gameMode.litematica_printer$destroyBlockPos())) {
            client.gameMode.stopDestroyBlock();
        }
    }

    public boolean inQueue(SchematicBlockContext ctx) {
        return inQueue(ctx.blockPos);
    }

    public void preprocess() {
        if (!ConfigUtils.isPrinterEnable()) {
            if (!breakQueue.isEmpty()) {
                breakQueue.clear();
                breakSet.clear();
            }
            if (breakPos != null) {
                breakPos = null;
            }
            this.selectionValidity = null;
        }
    }

    public boolean isNeedHandle() {
        return !breakQueue.isEmpty() || breakPos != null;
    }

    public void onTick() {
        if (QuickShulkerUtils.isOpenHandler()) return;
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
                breakSet.remove(pos);
                // 队列中的目标可能来自旧的渲染层/选区，执行前按当前配置复查
                this.setSelectionValidity(() -> ConfigUtils.isPositionInSelectionRange(
                        client.player, pos, Configs.Print.PRINT_SELECTION_TYPE));
                if (!canWorkAt(pos) || !canBreakBlock(pos) || !breakRestriction(level.getBlockState(pos))) {
                    continue;
                }
                if (ModUtils.isTweakerooLoaded()) {
                    if (ModUtils.isToolSwitchEnabled()) {
                        ModUtils.trySwitchToEffectiveTool(pos);
                    }
                }
                BlockBreakResult breakResult = continueDestroyBlock(pos, Direction.DOWN);
                if (breakResult == BlockBreakResult.IN_PROGRESS) {
                    breakPos = pos;
                    break;
                } else if (breakResult == BlockBreakResult.COMPLETED) {
                    break;
                }
            }
        } else if (continueDestroyBlock(breakPos, Direction.DOWN) != BlockBreakResult.IN_PROGRESS) {
            breakPos = null;
        }
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction) {
        // 所有入口（新目标、队列和持续挖掘）在切工具或发送数据包前复查当前位置。
        if (client.player == null || client.level == null || client.gameMode == null || !canWorkAt(blockPos)) {
            if (isBreaking(blockPos)) stopBreaking();
            return BlockBreakResult.FAILED;
        }
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        BlockBreakResult result = gameMode.litematica_printer$continueDestroyBlock(localPrediction, blockPos, direction);
        if (result == BlockBreakResult.IN_PROGRESS) {
            breakPos = blockPos;
        } else if (isBreaking(blockPos)) {
            breakPos = null;
            this.selectionValidity = null;
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