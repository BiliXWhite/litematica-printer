package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.mods.ModLoadUtils;
import me.aleksilassila.litematica.printer.utils.mods.TweakerooUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;

@SuppressWarnings({"DataFlowIssue", "BooleanMethodIsAlwaysInverted"})
@Environment(EnvType.CLIENT)
public final class InteractionUtils implements RuntimeComponent {
    public static final Minecraft client = Minecraft.getInstance();
    private static final UsageRestrictionCache BREAK_RESTRICTION_CACHE = new UsageRestrictionCache();

    private final BreakQueueState breakState = new BreakQueueState();

    public InteractionUtils() {
    }

    public static InteractionUtils getRuntime() {
        return RuntimeAccess.get().interactionUtils();
    }

    public static boolean canBreakBlock(BlockPos pos) {
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        if (world == null || player == null || client.gameMode == null) return false;
        BlockState currentState = world.getBlockState(pos);
        if (Configs.Break.BREAK_CHECK_HARDNESS.getBooleanValue()
                && currentState.getDestroySpeed(world, pos) < 0.0F) {
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
        if (Configs.Break.BREAK_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModLoadUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = TweakerooUtils.getBreakRestrictionListType();
            return BREAK_RESTRICTION_CACHE.allows("break_tweakeroo", listType,
                    TweakerooUtils.getBreakRestrictionBlacklist(),
                    TweakerooUtils.getBreakRestrictionWhitelist(),
                    blockState);
        } else {
            IConfigOptionListEntry optionListValue = Configs.Break.BREAK_LIMIT.getOptionListValue();
            UsageRestriction.ListType listType = optionListValue instanceof UsageRestriction.ListType type
                    ? type
                    : UsageRestriction.ListType.NONE;
            return BREAK_RESTRICTION_CACHE.allows("break_custom", listType,
                    Configs.Break.BREAK_BLACKLIST.getStrings(),
                    Configs.Break.BREAK_WHITELIST.getStrings(),
                    blockState);
        }
    }

    public void add(BlockPos pos) {
        this.breakState.add(pos);
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public void preprocess() {
        this.breakState.tickMarkers();
        this.breakState.clearIfDisabled(ConfigUtils.isEnable());
    }

    public void resetRuntime() {
        this.breakState.reset();
    }

    @Override public void onEpochChanged(RuntimeEvent.EpochChanged event) { this.resetRuntime(); }

    static boolean canAdvanceBreakQueue(java.util.function.Predicate<ResourceLease> held,
                                        boolean switching, boolean carrying) {
        return !switching && !carrying && !held.test(ResourceLease.MAIN_HAND)
                && !held.test(ResourceLease.INVENTORY) && !held.test(ResourceLease.CONTAINER);
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }
        if (this.breakState.isLocked()) {
            return;
        }
        if (!canAdvanceBreakQueue(RuntimeAccess.get().actionBroker()::isResourceHeld,
                RuntimeAccess.get().inventorySwitchGuard().isWaiting(),
                !player.containerMenu.getCarried().isEmpty())) {
            return;
        }
        if (this.breakState.activePos() == null && !this.breakState.hasQueued()) {
            return;
        }
        if (this.breakState.activePos() == null) {
            while (this.breakState.hasQueued()) {
                BlockPos pos = this.breakState.pollQueued();
                if (pos == null) {
                    continue;
                }
                if (!ConfigUtils.canInteracted(pos) || !canBreakBlock(pos) || !breakRestriction(level.getBlockState(pos))) {
                    continue;
                }
                BlockBreakResult result = continueDestroyBlock(pos, Direction.DOWN);
                if (result == BlockBreakResult.IN_PROGRESS) {
                    this.breakState.activePos(pos);
                    break;
                }
                if (result == BlockBreakResult.COMPLETED || result == BlockBreakResult.COMPLETED_WAIT) {
                    this.markRecentlyBroken(pos);
                    if (result == BlockBreakResult.COMPLETED_WAIT) {
                        this.markPendingBroken(pos, ConfigUtils.getBreakCooldown());
                    }
                }
            }
        } else {
            BlockPos activePos = this.breakState.activePos();
            // 检查当前目标是否仍可破坏（如冰挖掘后生成水/流体，流体不可破坏）
            if (!canBreakBlock(activePos)) {
                this.breakState.clearActive();
                onTick();
                return;
            }
            BlockBreakResult result = continueDestroyBlock(activePos, Direction.DOWN);
            if (result != BlockBreakResult.IN_PROGRESS) {
                if (result == BlockBreakResult.COMPLETED || result == BlockBreakResult.COMPLETED_WAIT) {
                    this.markRecentlyBroken(activePos);
                    if (result == BlockBreakResult.COMPLETED_WAIT) {
                        this.markPendingBroken(activePos, ConfigUtils.getBreakCooldown());
                    }
                }
                this.breakState.clearActive();
                onTick();
            }
        }
    }

    public boolean hasActiveDestroyTarget() {
        return this.breakState.activePos() != null;
    }

    public boolean isActiveDestroyTarget(BlockPos pos) {
        return pos != null && pos.equals(this.breakState.activePos());
    }

    public void suppressQueuedBreaks(int ticks) {
        this.breakState.suppress(ticks);
    }

    public void markRecentlyBroken(BlockPos pos) {
        this.breakState.markRecentlyBroken(pos);
    }

    public void markPendingBroken(BlockPos pos, int timeoutTicks) {
        this.breakState.markPendingBroken(pos, timeoutTicks);
    }

    public void confirmServerBlockUpdate(BlockPos pos) {
        this.breakState.confirmServerBlockUpdate(pos);
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (gameMode != null) {
            gameMode.litematica_printer$confirmServerBlockUpdate(pos);
        }
    }

    public void clearPendingBroken(BlockPos pos) {
        this.breakState.clearPendingBroken(pos);
    }

    public boolean isRecentlyBroken(BlockPos pos) {
        return this.breakState.isRecentlyBroken(pos);
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction, boolean trackBreakPos) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (gameMode == null || blockPos == null || direction == null) {
            return BlockBreakResult.FAILED;
        }
        BlockBreakResult result = gameMode.litematica_printer$continueDestroyBlock(
                localPrediction, blockPos, direction, this.breakState.forceDelayedDestroy());
        if (trackBreakPos && result == BlockBreakResult.IN_PROGRESS) {
            this.breakState.activePos(blockPos);
        }
        if (result != BlockBreakResult.IN_PROGRESS) {
            this.breakState.forceDelayedDestroy(false);
        }
        return result;
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction) {
        return this.continueDestroyBlock(blockPos, direction, localPrediction, true);
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlock(blockPos, direction, true);
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos) {
        return this.continueDestroyBlock(blockPos, Direction.DOWN);
    }

    public BlockBreakResult continueDestroyBlockForMine(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlockForMine(blockPos, direction, true);
    }

    public BlockBreakResult continueDestroyBlockForMine(BlockPos blockPos, Direction direction, boolean allowToolSwitch) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (gameMode == null) {
            return BlockBreakResult.FAILED;
        }
        return gameMode.litematica_printer$continueDestroyBlockForMine(blockPos, direction, allowToolSwitch);
    }

    public BlockBreakResult continueDestroyBlockForMine(BlockPos blockPos) {
        return this.continueDestroyBlockForMine(blockPos, Direction.DOWN);
    }

    public boolean isPendingDelayedDestroy(BlockPos blockPos) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        return gameMode != null && gameMode.litematica_printer$isPendingDelayedDestroy(blockPos);
    }

    public BlockBreakResult continueDestroyBlockWithoutTracking(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlock(blockPos, direction, true, false);
    }

    public BlockBreakResult continueDestroyBlockWithoutTracking(BlockPos blockPos) {
        return this.continueDestroyBlockWithoutTracking(blockPos, Direction.DOWN);
    }

    public BlockBreakResult continueDestroyBlockWithoutToolSwitch(BlockPos blockPos, Direction direction, boolean trackBreakPos) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (gameMode == null) {
            return BlockBreakResult.FAILED;
        }
        BlockBreakResult result = gameMode.litematica_printer$continueDestroyBlock(
                true,
                blockPos,
                direction,
                this.breakState.forceDelayedDestroy(),
                false
        );
        if (trackBreakPos && result == BlockBreakResult.IN_PROGRESS) {
            this.breakState.activePos(blockPos);
        }
        if (result != BlockBreakResult.IN_PROGRESS) {
            this.breakState.forceDelayedDestroy(false);
        }
        return result;
    }

    public BlockBreakResult continueDestroyBlockWithoutToolSwitch(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlockWithoutToolSwitch(blockPos, direction, true);
    }

    public InteractionResult useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        if (gameMode == null || hand == null || blockHit == null) {
            return InteractionResult.FAIL;
        }
        return gameMode.litematica_printer$useItemOn(localPrediction, hand, blockHit);
    }

    public InteractionResult useItemOn(InteractionHand hand, BlockHitResult blockHit) {
        return this.useItemOn(true, hand, blockHit);
    }
}
