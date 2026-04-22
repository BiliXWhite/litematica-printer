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

        processCleanupQueue();

        if (BedrockInventory.warningMessage() != null) {
            return;
        }

        int executeBudget = getExecuteBudget();
        if (!TARGETS.isEmpty()) {
            BedrockDebugLog.write("controller tick targets=" + TARGETS.size() + " budget=" + executeBudget);
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
                if (shouldRetireOutOfRange(status)) {
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
            }

            boolean retireOnSuccessfulRetracting = status == BedrockTarget.Status.RETRACTING
                    && !BedrockTargetBlocks.isTargetBlock(level.getBlockState(target.getBedrockPos()));
            if (status == BedrockTarget.Status.RETRACTED
                    || status == BedrockTarget.Status.FAILED
                    || status == BedrockTarget.Status.STUCK
                    || retireOnSuccessfulRetracting) {
                BedrockDebugLog.write("controller cleanup start bedrock=" + BedrockDebugLog.pos(target.getBedrockPos()) + " status=" + status);
                iterator.remove();
                for (BlockPos tempPos : target.getCleanupPositions()) {
                    cleanupBlockOrQueue(tempPos, !target.usesConservativeSync());
                }
            }
        }
        
        if (executeBudget < getExecuteBudget()) {
            scheduleNextExecuteWindow();
        }
    }

    public static boolean canAccept(BlockPos pos) {
        int maxTotal = 32; // Balanced capacity
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

        if (!canAccept(pos)) return false;

        BedrockTarget target = new BedrockTarget(pos, level);
        if (target.getStatus() == BedrockTarget.Status.FAILED) return false;

        BedrockTarget conflict = findConflictTarget(target);
        if (conflict != null) {
            cleanupRejectedTarget(target);
            return false;
        }

        TARGETS.add(target);
        BedrockDebugLog.write("submit accepted bedrock=" + BedrockDebugLog.pos(pos));
        return true;
    }

    private static void addToCleanup(BlockPos pos, boolean predictRemoval) {
        if (pos != null) {
            CLEANUP_QUEUE.add(pos);
            if (!predictRemoval) CONSERVATIVE_CLEANUP.add(pos);
        }
    }

    private static void processCleanupQueue() {
        if (CLEANUP_QUEUE.isEmpty()) return;
        int limit = 16; // Moderate cleanup speed
        int count = 0;
        List<BlockPos> toBack = new ArrayList<>();
        Iterator<BlockPos> iterator = CLEANUP_QUEUE.iterator();
        while (iterator.hasNext() && count < limit) {
            BlockPos pos = iterator.next();
            if (CLIENT.level == null) break;
            var state = CLIENT.level.getBlockState(pos);
            if (state.isAir() || !BedrockTargetBlocks.isCleanupResidue(state)) {
                iterator.remove();
                CONSERVATIVE_CLEANUP.remove(pos);
                continue;
            }
            if (isReservedByActiveTarget(pos)) continue;
            if (!CooldownUtils.INSTANCE.isOnCooldown(CLIENT.level, "cleanup_retry", pos)) {
                BedrockBreaker.breakBlock(pos, !CONSERVATIVE_CLEANUP.contains(pos));
                CooldownUtils.INSTANCE.setCooldown(CLIENT.level, "cleanup_retry", pos, 5);
                count++;
                iterator.remove();
                toBack.add(pos);
            }
        }
        if (!toBack.isEmpty()) CLEANUP_QUEUE.addAll(toBack);
    }

    private static int getExecuteBudget() {
        long now = ClientPlayerTickManager.getCurrentHandlerTime();
        if (now < nextExecuteTick) return 0;
        int budget = Configs.Break.BEDROCK_BLOCKS_PER_TICK.getIntegerValue();
        return budget <= 0 ? 6 : budget;
    }

    private static BedrockTarget findConflictTarget(BedrockTarget candidate) {
        Set<BlockPos> candidateFootprint = candidate.getReservedPositions();
        for (BedrockTarget existing : TARGETS) {
            Set<BlockPos> existingFootprint = existing.getReservedPositions();
            for (BlockPos pos : candidateFootprint) {
                if (existingFootprint.contains(pos)) return existing;
            }
        }
        return null;
    }

    private static boolean shouldRetireOutOfRange(BedrockTarget.Status status) {
        return status == BedrockTarget.Status.UNINITIALIZED || status == BedrockTarget.Status.RETRACTED || status == BedrockTarget.Status.FAILED || status == BedrockTarget.Status.STUCK;
    }

    private static void cleanupRejectedTarget(BedrockTarget target) {
        for (BlockPos pos : target.getCleanupPositions()) cleanupBlockOrQueue(pos, !target.usesConservativeSync());
    }

    private static void cleanupBlockOrQueue(BlockPos pos, boolean predictRemoval) {
        if (pos == null) return;
        addToCleanup(pos, predictRemoval);
        if (CLIENT.level == null || isReservedByActiveTarget(pos)) return;
        if (BedrockTargetBlocks.isCleanupResidue(CLIENT.level.getBlockState(pos))) {
            BedrockBreaker.breakBlock(pos, predictRemoval);
        }
    }

    private static boolean isReservedByActiveTarget(BlockPos pos) {
        for (BedrockTarget target : TARGETS) {
            if (target.getReservedPositions().contains(pos)) return true;
        }
        return false;
    }

    private static void scheduleNextExecuteWindow() {
        int interval = Math.max(0, Configs.Break.BEDROCK_INTERVAL.getIntegerValue());
        nextExecuteTick = ClientPlayerTickManager.getCurrentHandlerTime() + interval;
    }
}
