package me.aleksilassila.litematica.printer.handler.handlers.print;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.BlockStateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

/** Owns one water/ice workflow independently from the full selection scan. */
public final class WaterPrintTask implements PrintTask {
    private static final int STALL_PADDING_TICKS = 8;
    private static final int MIN_STALL_TICKS = 12;
    private static final int MAX_STALL_TICKS = 40;

    private final BlockPos pos;
    private final LongSupplier tickClock;
    private WaterTaskStage stage = WaterTaskStage.RESERVED;
    private long stageSinceTick;
    private long retryAtTick;
    private long actionGeneration;

    private WaterPrintTask(BlockPos pos, LongSupplier tickClock) {
        this.pos = pos.immutable();
        this.tickClock = tickClock;
        this.stageSinceTick = tickClock.getAsLong();
    }

    @Nullable
    public static WaterPrintTask tryCreate(SchematicBlockContext context, LongSupplier tickClock) {
        return isCandidate(context) ? new WaterPrintTask(context.blockPos, tickClock) : null;
    }

    @Override
    public BlockPos pos() {
        return this.pos;
    }

    WaterTaskStage stage() {
        return this.stage;
    }

    boolean isBreaking() {
        return Configs.Print.PRINT_ICE_FOR_WATER.getBooleanValue()
                && (this.stage == WaterTaskStage.REMOVE_EXISTING || this.stage == WaterTaskStage.BREAK_ICE);
    }

    @Override
    public boolean shouldKeep(ClientLevel level, WorldSchematic schematic) {
        if (!Configs.Print.PRINT_ICE_FOR_WATER.getBooleanValue()) {
            return false;
        }
        BlockState requiredState = schematic.getBlockState(this.pos);
        if (!BlockStateUtils.isWaterBlock(requiredState) || shouldSkipWaterloggedTarget(requiredState)) {
            return false;
        }
        this.reconcile(level, requiredState, level.getBlockState(this.pos));
        return this.stage != WaterTaskStage.COMPLETE;
    }

    @Override
    public boolean isWaitingForWorldUpdate(ClientLevel level, WorldSchematic schematic) {
        BlockState requiredState = schematic.getBlockState(this.pos);
        BlockState currentState = level.getBlockState(this.pos);
        this.reconcile(level, requiredState, currentState);
        boolean waitingForSupport = this.stage == WaterTaskStage.PLACE_ICE
                && !canStartIceWaterWorkflow(level, this.pos);
        if (waitingForSupport) {
            this.retryAtTick = Math.max(this.retryAtTick, this.tickClock.getAsLong() + 5L);
        }
        return this.stage.waitsForWorldUpdate()
                || this.stage == WaterTaskStage.RETRY_WAIT
                || waitingForSupport;
    }

    @Override
    public long nextCheckTick() {
        if (this.stage == WaterTaskStage.RETRY_WAIT) return this.retryAtTick;
        if (this.stage.waitsForWorldUpdate()) return this.stageSinceTick + MIN_STALL_TICKS;
        if (this.stage == WaterTaskStage.PLACE_ICE && this.retryAtTick > this.tickClock.getAsLong()) {
            return this.retryAtTick;
        }
        return this.tickClock.getAsLong();
    }

    @Override
    public PrintTaskBuildResult buildAction(SchematicBlockContext context) {
        this.reconcile(context.level, context.requiredState, context.currentState);
        return switch (this.stage) {
            case COMPLETE -> PrintTaskBuildResult.SKIP;
            case REMOVE_EXISTING -> this.breakBlockForWorkflow(context, false);
            case PLACE_ICE -> this.buildIcePlacement(context);
            case BREAK_ICE -> this.breakBlockForWorkflow(context, true);
            case PLACE_FINAL_BLOCK -> PrintTaskBuildResult.PASS;
            case RESERVED, WAIT_REMOVE_CONFIRM, WAIT_ICE_CONFIRM,
                    WAIT_WATER_CONFIRM, WAIT_FINAL_CONFIRM, RETRY_WAIT -> PrintTaskBuildResult.SKIP;
        };
    }

    @Override
    public @Nullable PrintTaskAction createActionHandle(SchematicBlockContext context, Action action) {
        this.reconcile(context.level, context.requiredState, context.currentState);
        if (this.stage != WaterTaskStage.PLACE_FINAL_BLOCK) {
            return null;
        }
        return this.newActionHandle(WaterTaskStage.PLACE_FINAL_BLOCK, WaterTaskStage.WAIT_FINAL_CONFIRM);
    }

    private PrintTaskBuildResult buildIcePlacement(SchematicBlockContext context) {
        if (!BlockStateUtils.isReplaceable(context.currentState)
                || !canStartIceWaterWorkflow(context.level, context.blockPos)) {
            return PrintTaskBuildResult.SKIP;
        }
        Action action = new Action().setItem(Items.ICE).setRequiresSupport();
        PrintTaskAction handle = this.newActionHandle(
                WaterTaskStage.PLACE_ICE,
                WaterTaskStage.WAIT_ICE_CONFIRM
        );
        return PrintTaskBuildResult.action(action, handle);
    }

    private PrintTaskBuildResult breakBlockForWorkflow(SchematicBlockContext context, boolean ice) {
        if (!InteractionUtils.canBreakBlock(context.blockPos)) {
            return PrintTaskBuildResult.SKIP;
        }
        if (ice && !IceBreakToolSelector.switchToNonSilkTouchBreakItem(context.client)) {
            return PrintTaskBuildResult.SKIP;
        }

        InteractionUtils.getRuntime().suppressQueuedBreaks(2);
        BlockBreakResult result = ice
                ? InteractionUtils.getRuntime().continueDestroyBlockWithoutToolSwitch(
                        context.blockPos, Direction.DOWN, false)
                : InteractionUtils.getRuntime().continueDestroyBlockWithoutTracking(
                        context.blockPos, Direction.DOWN);
        if (result == BlockBreakResult.COMPLETED || result == BlockBreakResult.COMPLETED_WAIT) {
            this.transition(ice
                    ? WaterTaskStage.WAIT_WATER_CONFIRM
                    : WaterTaskStage.WAIT_REMOVE_CONFIRM);
            return PrintTaskBuildResult.SKIP;
        }
        if (result == BlockBreakResult.IN_PROGRESS) {
            return PrintTaskBuildResult.SKIP;
        }
        this.retrySoon();
        return PrintTaskBuildResult.SKIP;
    }

    private PrintTaskAction newActionHandle(WaterTaskStage expected, WaterTaskStage success) {
        long token = ++this.actionGeneration;
        return new WaterTaskAction(token, expected, success);
    }

    private void reconcile(ClientLevel level, BlockState requiredState, BlockState currentState) {
        if (this.stage == WaterTaskStage.COMPLETE) {
            return;
        }
        long now = this.tickClock.getAsLong();
        WaterStageResolver.Observation observation = observe(level, requiredState, currentState);
        this.transition(WaterStageResolver.resolve(
                this.stage,
                observation,
                isWaterloggedTarget(requiredState),
                this.isStageStalled(level, currentState),
                now >= this.retryAtTick
        ));
    }

    private WaterStageResolver.Observation observe(
            ClientLevel level,
            BlockState requiredState,
            BlockState currentState
    ) {
        if (isWorkflowComplete(requiredState, currentState)) {
            return WaterStageResolver.Observation.COMPLETE;
        }
        if (BlockStateUtils.isCorrectWaterLevel(requiredState, currentState)) {
            return WaterStageResolver.Observation.WATER_READY;
        }
        if (currentState.getBlock() instanceof IceBlock) {
            return WaterStageResolver.Observation.ICE;
        }
        if (BlockStateUtils.isReplaceable(currentState)) {
            return canStartIceWaterWorkflow(level, this.pos)
                    ? WaterStageResolver.Observation.REPLACEABLE_WITH_SUPPORT
                    : WaterStageResolver.Observation.REPLACEABLE_WITHOUT_SUPPORT;
        }
        return WaterStageResolver.Observation.BLOCKED;
    }

    private void retrySoon() {
        this.retryAtTick = this.tickClock.getAsLong() + 1L;
        this.transition(WaterTaskStage.RETRY_WAIT);
    }

    private void transition(WaterTaskStage next) {
        if (this.stage == next) {
            return;
        }
        this.stage = next;
        this.stageSinceTick = this.tickClock.getAsLong();
        if (next == WaterTaskStage.COMPLETE) {
            this.actionGeneration++;
        }
    }

    private boolean isStageStalled(ClientLevel level, BlockState currentState) {
        long elapsed = this.tickClock.getAsLong() - this.stageSinceTick;
        return elapsed > getStallLimit(level, currentState);
    }

    private int getStallLimit(ClientLevel level, BlockState currentState) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return MIN_STALL_TICKS;
        }
        float progressPerTick = currentState.getDestroyProgress(player, level, this.pos);
        if (progressPerTick <= 0.0F) {
            return MIN_STALL_TICKS;
        }
        int estimatedTicks = (int) Math.ceil(1.0F / progressPerTick);
        return Math.max(MIN_STALL_TICKS, Math.min(MAX_STALL_TICKS, estimatedTicks + STALL_PADDING_TICKS));
    }

    private static boolean isCandidate(SchematicBlockContext context) {
        if (!BlockStateUtils.isWaterBlock(context.requiredState)
                || context.client.gameMode == null
                || context.client.gameMode.getPlayerMode().isCreative()
                || shouldSkipWaterloggedTarget(context.requiredState)
                || !Configs.Print.PRINT_ICE_FOR_WATER.getBooleanValue()
                || isWorkflowComplete(context.requiredState, context.currentState)) {
            return false;
        }
        if (context.currentState.getBlock() instanceof IceBlock) {
            return isWaterloggedTarget(context.requiredState)
                    || canIceBecomeWaterSource(context.level, context.blockPos);
        }
        if (isWaterloggedTarget(context.requiredState)
                && BlockStateUtils.isCorrectWaterLevel(context.requiredState, context.currentState)) {
            return true;
        }
        if (isDryWaterloggedBlock(context.requiredState, context.currentState)) {
            return true;
        }
        if (isWaterloggedTarget(context.requiredState) && !BlockStateUtils.isReplaceable(context.currentState)) {
            return true;
        }
        return isWaterloggedTarget(context.requiredState)
                || BlockStateUtils.isReplaceable(context.currentState)
                && canStartIceWaterWorkflow(context.level, context.blockPos);
    }

    private static boolean canStartIceWaterWorkflow(ClientLevel level, BlockPos pos) {
        return canIceBecomeWaterSource(level, pos);
    }

    private static boolean canIceBecomeWaterSource(ClientLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        //#if MC > 11904
        return !belowState.getCollisionShape(level, belowPos, CollisionContext.empty()).isEmpty()
                || !belowState.getFluidState().isEmpty();
        //#else
        //$$ return belowState.getMaterial().blocksMotion() || belowState.getMaterial().isLiquid();
        //#endif
    }

    private static boolean shouldSkipWaterloggedTarget(BlockState requiredState) {
        return Configs.Print.SKIP_WATERLOGGED_BLOCK.getBooleanValue() && isWaterloggedTarget(requiredState);
    }

    private static boolean isWaterloggedTarget(BlockState requiredState) {
        return requiredState.hasProperty(BlockStateProperties.WATERLOGGED)
                && requiredState.getValue(BlockStateProperties.WATERLOGGED);
    }

    private static boolean isCurrentWaterlogged(BlockState currentState) {
        return currentState.hasProperty(BlockStateProperties.WATERLOGGED)
                && currentState.getValue(BlockStateProperties.WATERLOGGED);
    }

    private static boolean isDryWaterloggedBlock(BlockState requiredState, BlockState currentState) {
        return isWaterloggedTarget(requiredState)
                && currentState.getBlock() == requiredState.getBlock()
                && !isCurrentWaterlogged(currentState);
    }

    private static boolean isWorkflowComplete(BlockState requiredState, BlockState currentState) {
        if (isWaterloggedTarget(requiredState)) {
            return currentState.getBlock() == requiredState.getBlock() && isCurrentWaterlogged(currentState);
        }
        return BlockStateUtils.isCorrectWaterLevel(requiredState, currentState);
    }

    private final class WaterTaskAction implements PrintTaskAction {
        private final long token;
        private final WaterTaskStage expected;
        private final WaterTaskStage success;

        private WaterTaskAction(long token, WaterTaskStage expected, WaterTaskStage success) {
            this.token = token;
            this.expected = expected;
            this.success = success;
        }

        @Override
        public BlockState expectedBlockState(SchematicBlockContext context, Action action) {
            return this.expected == WaterTaskStage.PLACE_ICE
                    ? Blocks.ICE.defaultBlockState()
                    : context.requiredState;
        }

        @Override
        public void onQueued(SchematicBlockContext context, Action action) {
            this.markSubmitted();
        }

        @Override
        public void onSuccess(SchematicBlockContext context, Action action) {
            this.markSubmitted();
        }

        private void markSubmitted() {
            if (this.token == actionGeneration
                    && (stage == this.expected || stage == this.success)) {
                transition(this.success);
            }
        }

        @Override
        public void onCancelled(SchematicBlockContext context, Action action) {
            if (this.token == actionGeneration) {
                retrySoon();
            }
        }

        @Override
        public void onFailure(SchematicBlockContext context, Action action) {
            if (this.token == actionGeneration) {
                retrySoon();
            }
        }
    }
}
