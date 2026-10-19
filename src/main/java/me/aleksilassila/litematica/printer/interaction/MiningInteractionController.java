package me.aleksilassila.litematica.printer.interaction;

import me.aleksilassila.litematica.printer.handler.handlers.MineDebugLog;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stateful mining protocol. The Mixin only exposes Minecraft fields through {@link MiningInteractionPort}. */
public final class MiningInteractionController {
    private static final int MIN_PENDING_DESTROY_TICKS = 8;
    private static final int MAX_PENDING_DESTROY_TICKS = 200;
    private static final int SAFETY_MARGIN = 5;

    private final MiningInteractionPort port;
    private final MiningFeedback feedback;
    private final ToolSwitchService toolSwitchService;
    private final Map<BlockPos, DelayedDestroyEntry> pendingDelayedDestroys = new LinkedHashMap<>();
    private int projectedDurabilityCost;
    private boolean suppressFastBreak;
    private BlockPos lastLoggedProgressPos;
    private long lastLoggedProgressTick = Long.MIN_VALUE;

    public MiningInteractionController(MiningInteractionPort port, ToolSwitchService toolSwitchService) {
        this.port = port;
        this.toolSwitchService = toolSwitchService;
        this.feedback = new MiningFeedback(port.client());
    }

    public void reset() {
        LocalPlayer player = this.port.client().player;
        if (this.port.isDestroying()) this.clearDestroyProgress(player, this.port.destroyPos());
        for (DelayedDestroyEntry entry : this.pendingDelayedDestroys.values()) {
            if (entry.pos != null) this.clearDestroyProgress(player, entry.pos);
        }
        this.port.isDestroying(false);
        this.port.destroyProgress(0.0F);
        this.port.destroyPos(BlockPos.ZERO);
        this.port.destroyingItem(ItemStack.EMPTY);
        this.pendingDelayedDestroys.clear();
        this.projectedDurabilityCost = 0;
        this.suppressFastBreak = false;
        this.lastLoggedProgressPos = null;
        this.lastLoggedProgressTick = Long.MIN_VALUE;
        this.feedback.reset();
    }

    public boolean isPendingDelayedDestroy(BlockPos pos) {
        return this.feedback.hasPending(pos);
    }

    public void confirmServerBlockUpdate(BlockPos pos) {
        if (pos == null || !this.feedback.hasPending(pos)) {
            return;
        }
        ClientLevel level = this.port.client().level;
        if (level != null && level.getBlockState(pos).isAir()) {
            this.feedback.removePending(pos);
            DelayedDestroyEntry removed = this.pendingDelayedDestroys.remove(pos);
            if (removed != null) {
                this.clearDestroyProgress(this.port.client().player, pos);
            }
        }
    }

    public void tick() {
        LocalPlayer player = this.port.client().player;
        ClientLevel level = this.port.client().level;
        if (player == null || level == null) return;
        this.feedback.cleanupPending(player, level, this.clientTick());
        this.projectedDurabilityCost = 0;
        this.suppressFastBreak = false;
        Iterator<Map.Entry<BlockPos, DelayedDestroyEntry>> iterator = this.pendingDelayedDestroys.entrySet().iterator();
        while (iterator.hasNext()) {
            DelayedDestroyEntry entry = iterator.next().getValue();
            BlockPos pos = entry.pos;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                this.feedback.flushPendingBreakSound(pos);
                this.feedback.removePending(pos);
                iterator.remove();
                this.clearDestroyProgress(player, pos);
                continue;
            }
            if (!entry.localPrediction) {
                int elapsed = (int) (this.clientTick() - entry.startTick);
                if (elapsed >= this.pendingDestroyTimeout(player, level, pos, state)) {
                    this.feedback.removePending(pos);
                    iterator.remove();
                }
                continue;
            }
            if (!this.feedback.hasPending(pos)) {
                iterator.remove();
                this.clearDestroyProgress(player, pos);
            }
        }
    }

    public BlockBreakResult continueForMine(BlockPos pos, Direction direction, boolean allowToolSwitch) {
        LocalPlayer player = this.port.client().player;
        ClientLevel level = this.port.client().level;
        if (player == null || level == null || this.port.client().gameMode == null) return BlockBreakResult.FAILED;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            this.feedback.flushPendingBreakSound(pos);
            this.feedback.removePending(pos);
            this.pendingDelayedDestroys.remove(pos);
            if (this.port.isDestroying() && this.port.matchesDestroyTarget(pos)) this.resetDestroyState(player, pos);
            this.feedback.resetHitSound();
            return BlockBreakResult.COMPLETED;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) return BlockBreakResult.FAILED;
        // A tool/config change must not let the fast path bypass an outstanding confirmation.
        if (this.pendingDelayedDestroys.containsKey(pos) || this.feedback.hasPending(pos)) {
            return BlockBreakResult.IN_PROGRESS;
        }
        if (this.port.isInventorySwitchPending()) {
            return BlockBreakResult.IN_PROGRESS;
        }
        if (this.toolSwitchService.prepareForBreak(pos, state, allowToolSwitch)
                != ToolPreparationResult.READY) {
            return BlockBreakResult.IN_PROGRESS;
        }
        this.port.ensureCarriedItemSent();
        float progress = state.getDestroyProgress(player, level, pos);
        // The server breaks on STOP when delta * (elapsed + 1) >= 0.7 regardless of any config.
        // Gating the fast path on the configurable threshold made blocks whose delta sits just below
        // the threshold (e.g. 0.777 < 0.78) fall into the slow path and get capped at 10/s.
        // Use the server's own 0.7 rule so mining speed no longer depends on the threshold value.
        boolean fast = player.getAbilities().instabuild
                || progress >= 0.7F;
        if (fast && !player.getAbilities().instabuild && this.toolSwitchService.isDurabilityGuardActive()) {
            ItemStack held = player.getMainHandItem();
            if (held.isDamageableItem()) {
                int remaining = held.getMaxDamage() - held.getDamageValue();
                if (remaining - this.projectedDurabilityCost <= SAFETY_MARGIN) {
                    fast = false;
                    // Hold the downgrade through continueDestroy so its fast gate doesn't re-enable it.
                    this.suppressFastBreak = true;
                }
            }
        }
        if (fast) {
            boolean delayed = delayedConfirmation(false);
            if (this.port.isDestroying()) this.resetDestroyState(player, this.port.destroyPos());
            this.feedback.resetHitSound();
            this.feedback.playHitSound(player, level, state, pos, true);
            if (progress >= 0.7F) {
                this.feedback.playBreakSound(pos, state);
            }
            if (delayed) this.queuePendingDestroy(pos, true);
            this.send(Action.START_DESTROY_BLOCK, pos, direction);
            if (!player.getAbilities().instabuild) {
                if (progress >= 1.0F && !delayed) {
                    NetworkUtils.sendPacket(sequence -> {
                        this.port.destroyBlock(pos);
                        return this.port.actionPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                    });
                } else {
                    this.send(Action.STOP_DESTROY_BLOCK, pos, direction);
                }
                ItemStack held = player.getMainHandItem();
                if (held.isDamageableItem()) {
                    this.projectedDurabilityCost++;
                }
            }
            this.feedback.resetHitSound();
            if (delayed) return BlockBreakResult.COMPLETED_WAIT;
            return player.getAbilities().instabuild || progress >= 1.0F
                    ? BlockBreakResult.COMPLETED
                    : BlockBreakResult.COMPLETED_WAIT;
        }
        BlockBreakResult result = this.continueDestroy(true, pos, direction, false, allowToolSwitch, true);
        if (result == BlockBreakResult.FAILED) return result;
        return this.pendingDelayedDestroys.containsKey(pos) || this.feedback.hasPending(pos)
                ? BlockBreakResult.IN_PROGRESS : result;
    }

    public BlockBreakResult continueDestroy(
            boolean localPrediction,
            BlockPos pos,
            Direction direction,
            boolean forceDelayedDestroy,
            boolean allowToolSwitch
    ) {
        return this.continueDestroy(
                localPrediction, pos, direction, forceDelayedDestroy, allowToolSwitch, false);
    }

    private BlockBreakResult continueDestroy(
            boolean localPrediction,
            BlockPos pos,
            Direction direction,
            boolean forceDelayedDestroy,
            boolean allowToolSwitch,
            boolean miningFeedback
    ) {
        LocalPlayer player = this.port.client().player;
        ClientLevel level = this.port.client().level;
        if (player == null || level == null || this.port.client().gameMode == null) return BlockBreakResult.FAILED;
        boolean delayedConfirmation = delayedConfirmation(forceDelayedDestroy);
        boolean localEffects = localPrediction || forceDelayedDestroy;
        boolean localRemoval = localPrediction && !delayedConfirmation;
        boolean manualSound = miningFeedback || forceDelayedDestroy || !localPrediction;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            this.feedback.flushPendingBreakSound(pos);
            this.feedback.removePending(pos);
            this.feedback.resetHitSound();
            MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(pos) + " path=air_or_liquid");
            return BlockBreakResult.COMPLETED;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            MineDebugLog.write("mine break failed pos=" + MineDebugLog.pos(pos) + " reason=out_of_world_border");
            return BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild) {
            if (manualSound) this.feedback.playHitSound(player, level, state, pos, true);
            NetworkUtils.sendPacket(sequence -> {
                if (localRemoval) this.port.destroyBlock(pos);
                return this.port.actionPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            this.feedback.resetHitSound();
            return BlockBreakResult.COMPLETED;
        }
        if (this.toolSwitchService.prepareForBreak(pos, state, allowToolSwitch)
                != ToolPreparationResult.READY) {
            return BlockBreakResult.IN_PROGRESS;
        }
        this.port.ensureCarriedItemSent();
        if (this.feedback.hasPending(pos) && !this.pendingDelayedDestroys.containsKey(pos)) {
            return BlockBreakResult.COMPLETED_WAIT;
        }
        if (this.pendingDelayedDestroys.containsKey(pos)) {
            return this.port.isDestroying() ? BlockBreakResult.IN_PROGRESS : BlockBreakResult.COMPLETED;
        }
        if (this.port.isDestroying() && !pos.equals(this.port.destroyPos())) this.abortPrevious(player, pos, direction);
        if (this.port.isDestroying() && pos.equals(this.port.destroyPos())) {
            this.port.destroyProgress(this.port.destroyProgress() + state.getDestroyProgress(player, level, pos));
            if (manualSound) this.feedback.playHitSound(player, level, state, pos, false);
            if (localEffects) level.destroyBlockProgress(player.getId(), pos, this.destroyStage());
            // Hold START open until the server-side elapsed makes delta*(elapsed+1) >= 0.7, then STOP
            // breaks immediately. This is normal mining (accumulation), not the same-tick exploit that
            // the old useDelayed branch used (which always failed to failedToMine for delta < 0.7 → 10/s).
            if (this.port.destroyProgress() >= completionThreshold(forceDelayedDestroy)) {
                if (miningFeedback || !localRemoval) this.feedback.playBreakSound(pos, state);
                NetworkUtils.sendPacket(sequence -> {
                    if (delayedConfirmation) this.queuePendingDestroy(pos, localPrediction);
                    if (localRemoval) this.port.destroyBlock(pos);
                    this.resetDestroyState(player, pos);
                    return this.port.actionPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                });
                if (localEffects) level.destroyBlockProgress(player.getId(), pos, -1);
                this.feedback.resetHitSound();
                return BlockBreakResult.COMPLETED;
            }
            this.logProgress(pos);
            return BlockBreakResult.IN_PROGRESS;
        }
        if (this.port.isDestroying()) this.abortPrevious(player, pos, direction);
        if (this.port.destroyProgress() == 0.0F && localEffects) state.attack(level, pos, player);
        if (manualSound) this.feedback.playHitSound(player, level, state, pos, true);
        float progress = state.getDestroyProgress(player, level, pos);
        // Server breaks on same-tick START+STOP whenever delta >= 0.7, independent of the
        // configurable threshold. Keep this fast path so delta>=0.7 blocks are never dragged
        // down to the hold path (and don't pay a per-tick packet cost either). The session-level
        // The session-level BREAK_BLOCKS_PER_TICK budget (MineToolSession) is the per-tick batch
        // ceiling; here only suppressFastBreak propagates the durability downgrade from
        // continueForMine so the Tweakeroo near-broken-tool protection is never punched through.
        if (!this.suppressFastBreak && progress >= completionThreshold(forceDelayedDestroy)) {
            if (miningFeedback || !localRemoval) this.feedback.playBreakSound(pos, state);
            this.send(Action.START_DESTROY_BLOCK, pos, direction);
            NetworkUtils.sendPacket(sequence -> {
                if (delayedConfirmation) this.queuePendingDestroy(pos, localPrediction);
                if (localRemoval) this.port.destroyBlock(pos);
                this.resetDestroyState(player, pos);
                return this.port.actionPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
            });
            if (localEffects) level.destroyBlockProgress(player.getId(), pos, -1);
            this.feedback.resetHitSound();
            return BlockBreakResult.COMPLETED;
        }
        if (!localPrediction) {
            // Server-authoritative callers (e.g. bedrock cleanup) need START before STOP so the
            // server tracks this position and can finish a slower break. The client waits for the
            // server update instead of removing the block locally. Cleanup handles several positions
            // per tick, so it cannot hold one mining session open across ticks.
            long startTick = this.clientTick();
            this.send(Action.START_DESTROY_BLOCK, pos, direction);
            NetworkUtils.sendPacket(sequence -> {
                this.pendingDelayedDestroys.put(pos.immutable(),
                        new DelayedDestroyEntry(pos.immutable(), false, startTick));
                this.feedback.addPending(pos, startTick);
                this.resetDestroyState(player, pos);
                return this.port.actionPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
            });
            level.destroyBlockProgress(player.getId(), pos, this.destroyStage());
            this.feedback.resetHitSound();
            return BlockBreakResult.COMPLETED_WAIT;
        }
        // delta < 0.7 with local prediction: open a hold-OPEN session (single server mining slot).
        // Each subsequent call on the same pos advances destroyProgress by delta until >= 0.7, then a
        // STOP breaks it through the server's accumulated elapsed. This is the only path that avoids
        // the failedToMine queue, which auto-breaks at progress>=1.0 → the 10/s single-slot dead end.
        this.send(Action.START_DESTROY_BLOCK, pos, direction);
        this.port.isDestroying(true);
        this.port.destroyPos(pos);
        this.port.destroyProgress(progress);
        this.port.destroyingItem(player.getMainHandItem());
        if (localEffects) level.destroyBlockProgress(player.getId(), pos, this.destroyStage());
        return BlockBreakResult.IN_PROGRESS;
    }

    private static boolean delayedConfirmation(boolean forceDelayedDestroy) {
        return forceDelayedDestroy || Configs.Break.BREAK_USE_DELAYED_DESTROY.getBooleanValue();
    }

    private static float completionThreshold(boolean forceDelayedDestroy) {
        return delayedConfirmation(forceDelayedDestroy) ? ConfigUtils.getBreakProgressThreshold() : 0.7F;
    }

    private void queuePendingDestroy(BlockPos pos, boolean localPrediction) {
        long startTick = this.clientTick();
        this.pendingDelayedDestroys.put(pos.immutable(),
                new DelayedDestroyEntry(pos.immutable(), localPrediction, startTick));
        this.feedback.addPending(pos, startTick);
    }

    private int pendingDestroyTimeout(
            LocalPlayer player,
            ClientLevel level,
            BlockPos pos,
            BlockState state
    ) {
        float progress = state.getDestroyProgress(player, level, pos);
        if (progress <= 0.0F) {
            return MAX_PENDING_DESTROY_TICKS;
        }
        int estimatedTicks = (int) Math.ceil(1.0F / progress);
        return Math.max(MIN_PENDING_DESTROY_TICKS, Math.min(estimatedTicks + 10, MAX_PENDING_DESTROY_TICKS));
    }

    private void abortPrevious(LocalPlayer player, BlockPos next, Direction direction) {
        BlockPos previous = this.port.destroyPos();
        this.feedback.clearPendingBreakSound(previous);
        NetworkUtils.sendPacket(this.port.actionPacket(Action.ABORT_DESTROY_BLOCK, previous, direction, 0));
        MineDebugLog.write("mine break abort previous=" + MineDebugLog.pos(previous) + " next=" + MineDebugLog.pos(next));
        this.resetDestroyState(player, previous);
        this.feedback.resetHitSound();
    }

    private void resetDestroyState(LocalPlayer player, BlockPos pos) {
        this.port.isDestroying(false);
        this.port.destroyProgress(0.0F);
        this.clearDestroyProgress(player, pos);
    }

    private void clearDestroyProgress(LocalPlayer player, BlockPos pos) {
        if (player == null || pos == null || this.port.client().level == null) return;
        try {
            this.port.client().level.destroyBlockProgress(player.getId(), pos, -1);
        } catch (IllegalStateException ignored) {
        }
    }

    private int destroyStage() {
        float progress = this.port.destroyProgress() >= 0.7F
                ? 1.0F : this.port.destroyProgress();
        return progress > 0.0F ? (int) (progress * 10.0F) : -1;
    }

    private void send(Action action, BlockPos pos, Direction direction) {
        NetworkUtils.sendPacket(sequence -> this.port.actionPacket(action, pos, direction, sequence));
    }

    private void logProgress(BlockPos pos) {
        long tick = this.clientTick();
        if (!pos.equals(this.lastLoggedProgressPos) || tick - this.lastLoggedProgressTick >= 10) {
            MineDebugLog.write("mine break in_progress pos=" + MineDebugLog.pos(pos)
                    + " progress=" + this.port.destroyProgress());
            this.lastLoggedProgressPos = pos;
            this.lastLoggedProgressTick = tick;
        }
    }

    private long clientTick() {
        ClientLevel level = this.port.client().level;
        return level == null ? 0L : level.getGameTime();
    }

    private static final class DelayedDestroyEntry {
        final BlockPos pos;
        final boolean localPrediction;
        final long startTick;

        DelayedDestroyEntry(BlockPos pos, boolean localPrediction, long startTick) {
            this.pos = pos;
            this.localPrediction = localPrediction;
            this.startTick = startTick;
        }
    }
}
