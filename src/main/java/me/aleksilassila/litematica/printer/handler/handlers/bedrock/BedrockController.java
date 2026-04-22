package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.CooldownUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BedrockController {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final List<BedrockTarget> TARGETS = new ArrayList<>();
    private static final Set<BlockPos> CLEANUP_QUEUE = new LinkedHashSet<>();
    private static final Set<BlockPos> CONSERVATIVE_CLEANUP = new LinkedHashSet<>();
    private static long nextAcceptTick = 0L;
    private static long nextExecuteTick = 0L;
    private static long lastProcessedTick = Long.MIN_VALUE;

    private BedrockController() {
    }

    public static void reset() {
        if (!TARGETS.isEmpty() || !CLEANUP_QUEUE.isEmpty()) {
            BedrockDebugLog.write("controller reset targets=" + TARGETS.size() + " cleanup=" + CLEANUP_QUEUE.size());
        }
        TARGETS.clear();
        CLEANUP_QUEUE.clear();
        CONSERVATIVE_CLEANUP.clear();
        nextAcceptTick = 0L;
        nextExecuteTick = 0L;
        lastProcessedTick = Long.MIN_VALUE;
    }

    public static void tick() {
        ClientLevel level = CLIENT.level;
        if (level == null) {
            reset();
            return;
        }

        long now = ClientPlayerTickManager.getCurrentHandlerTime();
        if (now == lastProcessedTick) {
            return;
        }
        lastProcessedTick = now;

        // 强力清理队列：死磕到底，变空气才移除
        processCleanupQueue();

        if (BedrockInventory.warningMessage() != null) {
            return;
        }

        int executeBudget = getExecuteBudget();
        if (!TARGETS.isEmpty()) {
            BedrockDebugLog.write("controller tick targets=" + TARGETS.size()
                    + " cleanup=" + CLEANUP_QUEUE.size()
                    + " budget=" + executeBudget
                    + " nextExecuteTick=" + nextExecuteTick
                    + " now=" + now);
        }
        Iterator<BedrockTarget> iterator = TARGETS.iterator();
        while (iterator.hasNext()) {
            BedrockTarget target = iterator.next();
            if (target == null) {
                iterator.remove();
                continue;
            }
            if (!ConfigUtils.canInteracted(target.getBedrockPos())) {
                BedrockTarget.Status status = target.refreshStatusOnly();
                BedrockDebugLog.write("controller out_of_range bedrock=" + BedrockDebugLog.pos(target.getBedrockPos())
                        + " status=" + status);
                if (shouldRetireOutOfRange(status)) {
                    BedrockDebugLog.write("controller cleanup start bedrock=" + BedrockDebugLog.pos(target.getBedrockPos())
                            + " status=" + status
                            + " cleanupCount=" + target.getCleanupPositions().size()
                            + " reason=out_of_range");
                    iterator.remove();
                    for (BlockPos tempPos : target.getCleanupPositions()) {
                        cleanupBlockOrQueue(tempPos, !target.usesConservativeSync());
                    }
                }
                continue;
            }

            BedrockTarget.Status status = target.tick(executeBudget > 0);
            if (target.executedThisTick()) {
                executeBudget--;
                scheduleNextExecuteWindow();
                BedrockDebugLog.write("controller execute consumed bedrock=" + BedrockDebugLog.pos(target.getBedrockPos())
                        + " remainingBudget=" + executeBudget
                        + " nextExecuteTick=" + nextExecuteTick);
            }

            boolean retireOnSuccessfulRetracting = status == BedrockTarget.Status.RETRACTING
                    && !BedrockTargetBlocks.isTargetBlock(level.getBlockState(target.getBedrockPos()));
            if (status == BedrockTarget.Status.RETRACTED
                    || status == BedrockTarget.Status.FAILED
                    || status == BedrockTarget.Status.STUCK
                    || retireOnSuccessfulRetracting) {
                BedrockDebugLog.write("controller cleanup start bedrock=" + BedrockDebugLog.pos(target.getBedrockPos())
                        + " status=" + status
                        + " cleanupCount=" + target.getCleanupPositions().size()
                        + (retireOnSuccessfulRetracting ? " reason=retracting_bedrock_gone" : ""));
                iterator.remove();
                for (BlockPos tempPos : target.getCleanupPositions()) {
                    cleanupBlockOrQueue(tempPos, !target.usesConservativeSync());
                }
            }
        }
    }

    public static boolean canAccept(BlockPos pos) {
        long now = ClientPlayerTickManager.getCurrentHandlerTime();
        if (now < nextAcceptTick) {
            return false;
        }

        int maxTotal = Configs.Break.BEDROCK_BLOCKS_PER_TICK.getIntegerValue();
        if (maxTotal <= 0) maxTotal = 64;

        if (TARGETS.size() >= maxTotal) return false;

        for (BedrockTarget target : TARGETS) {
            if (target.getBedrockPos().equals(pos)) return false;
            if (target.getPistonPos().equals(pos)) return false;
        }
        return true;
    }

    public static boolean submit(BlockPos pos) {
        ClientLevel level = CLIENT.level;
        if (level == null || !BedrockTargetBlocks.isTargetBlock(level.getBlockState(pos))) return false;

        if (!canAccept(pos)) {
            BedrockDebugLog.write("submit rejected bedrock=" + BedrockDebugLog.pos(pos) + " reason=cannot_accept");
            return false;
        }

        BedrockTarget target = new BedrockTarget(pos, level);
        if (target.getStatus() == BedrockTarget.Status.FAILED) {
            BedrockDebugLog.write("submit failed bedrock=" + BedrockDebugLog.pos(pos) + " reason=target_failed_on_create");
            return false;
        }

        BedrockTarget conflict = findConflictTarget(target);
        if (conflict != null) {
            BedrockDebugLog.write("submit rejected bedrock=" + BedrockDebugLog.pos(pos)
                    + " reason=machine_overlap"
                    + " conflictBedrock=" + BedrockDebugLog.pos(conflict.getBedrockPos())
                    + " torchSupport=" + BedrockDebugLog.pos(target.getTorchSupportPos())
                    + " piston=" + BedrockDebugLog.pos(target.getPistonPos()));
            cleanupRejectedTarget(target);
            return false;
        }

        if (target.getStatus() != BedrockTarget.Status.FAILED) {
            TARGETS.add(target);
            BedrockDebugLog.write("submit accepted bedrock=" + BedrockDebugLog.pos(pos)
                    + " piston=" + BedrockDebugLog.pos(target.getPistonPos())
                    + " torchSupport=" + BedrockDebugLog.pos(target.getTorchSupportPos())
                    + " slime=" + BedrockDebugLog.pos(target.getSlimePos())
                    + " conservativeSync=" + target.usesConservativeSync());
            return true;
        }
        return false;
    }

    private static void addToCleanup(BlockPos pos) {
        addToCleanup(pos, true);
    }

    private static void addToCleanup(BlockPos pos, boolean predictRemoval) {
        if (pos != null) {
            CLEANUP_QUEUE.add(pos);
            if (!predictRemoval) {
                CONSERVATIVE_CLEANUP.add(pos);
            }
        }
    }

    private static void processCleanupQueue() {
        if (CLEANUP_QUEUE.isEmpty()) return;

        int limit = 240; // push cleanup throughput to avoid residue accumulation
        int count = 0;
        Iterator<BlockPos> iterator = CLEANUP_QUEUE.iterator();

        while (iterator.hasNext() && count < limit) {
            BlockPos pos = iterator.next();
            if (CLIENT.level == null) break;

            var state = CLIENT.level.getBlockState(pos);
            if (state.isAir()) {
                iterator.remove();
                CONSERVATIVE_CLEANUP.remove(pos);
                continue;
            }

            // Cleanup only touches machine residues to avoid touching user structures.
            if (!BedrockTargetBlocks.isCleanupResidue(state)) {
                iterator.remove();
                CONSERVATIVE_CLEANUP.remove(pos);
                continue;
            }

            if (isReservedByActiveTarget(pos)) {
                continue;
            }

            if (!CooldownUtils.INSTANCE.isOnCooldown(CLIENT.level, "cleanup_retry", pos)) {
                boolean predictRemoval = !CONSERVATIVE_CLEANUP.contains(pos);
                BedrockBreaker.breakBlock(pos, predictRemoval);
                CooldownUtils.INSTANCE.setCooldown(CLIENT.level, "cleanup_retry", pos, 1);
                BedrockDebugLog.write("cleanup retry pos=" + BedrockDebugLog.pos(pos)
                        + " state=" + BedrockDebugLog.describeState(state)
                        + " predictRemoval=" + predictRemoval);
                count++;
            }
        }
    }

    private static int getExecuteBudget() {
        long now = ClientPlayerTickManager.getCurrentHandlerTime();
        if (now < nextExecuteTick) {
            return 0;
        }
        int budget = Configs.Break.BEDROCK_BLOCKS_PER_TICK.getIntegerValue();
        return budget <= 0 ? 64 : budget;
    }

    private static BedrockTarget findConflictTarget(BedrockTarget candidate) {
        Set<BlockPos> candidateFootprint = candidate.getReservedPositions();
        for (BedrockTarget existing : TARGETS) {
            Set<BlockPos> existingFootprint = existing.getReservedPositions();
            for (BlockPos pos : candidateFootprint) {
                if (existingFootprint.contains(pos)) {
                    return existing;
                }
            }
        }
        return null;
    }

    private static boolean shouldRetireOutOfRange(BedrockTarget.Status status) {
        return status == BedrockTarget.Status.RETRACTED
                || status == BedrockTarget.Status.FAILED
                || status == BedrockTarget.Status.STUCK
                || status == BedrockTarget.Status.EXTENDED
                || status == BedrockTarget.Status.RETRACTING
                || status == BedrockTarget.Status.NEEDS_WAITING
                || status == BedrockTarget.Status.UNEXTENDED_WITH_POWER_SOURCE
                || status == BedrockTarget.Status.UNEXTENDED_WITHOUT_POWER_SOURCE;
    }

    private static void cleanupRejectedTarget(BedrockTarget target) {
        for (BlockPos pos : target.getCleanupPositions()) {
            cleanupBlockOrQueue(pos, !target.usesConservativeSync());
        }
    }

    private static void cleanupBlockOrQueue(BlockPos pos, boolean predictRemoval) {
        if (pos == null) {
            return;
        }

        addToCleanup(pos, predictRemoval);
        if (CLIENT.level == null) {
            return;
        }
        if (isReservedByActiveTarget(pos)) {
            BedrockDebugLog.write("cleanup deferred pos=" + BedrockDebugLog.pos(pos) + " reason=reserved_by_active_target");
            return;
        }
        if (BedrockTargetBlocks.isCleanupResidue(CLIENT.level.getBlockState(pos))) {
            BedrockBreaker.breakBlock(pos, predictRemoval);
        }
    }

    private static boolean isReservedByActiveTarget(BlockPos pos) {
        for (BedrockTarget target : TARGETS) {
            if (target.getReservedPositions().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleNextExecuteWindow() {
        int interval = Math.max(0, Configs.Break.BEDROCK_INTERVAL.getIntegerValue());
        if (interval <= 0) {
            return;
        }
        nextExecuteTick = ClientPlayerTickManager.getCurrentHandlerTime() + interval;
        BedrockDebugLog.write("controller schedule interval=" + interval + " nextExecuteTick=" + nextExecuteTick);
    }
}
