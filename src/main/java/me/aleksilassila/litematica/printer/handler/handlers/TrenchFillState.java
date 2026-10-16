package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Target, retry and rate history for mine trench filling. Minecraft interactions stay in MineHandler. */
final class TrenchFillState {
    private final Set<BlockPos> fillTargets = new LinkedHashSet<>();
    private final Set<BlockPos> waterloggedTargets = new LinkedHashSet<>();
    private final Map<BlockPos, Long> inFlight = new HashMap<>();
    private final Map<BlockPos, Long> retryAt = new HashMap<>();
    private List<PrinterBox> scopeBoxes = List.of();
    private int sentThisTick;
    private long lastSentTick = Long.MIN_VALUE;

    void clear() {
        this.fillTargets.clear();
        this.waterloggedTargets.clear();
        this.inFlight.clear();
        this.retryAt.clear();
        this.scopeBoxes = List.of();
        this.sentThisTick = 0;
        this.lastSentTick = Long.MIN_VALUE;
    }

    void beginTick() {
        this.sentThisTick = 0;
    }

    boolean hasWork() {
        return !this.fillTargets.isEmpty() || !this.waterloggedTargets.isEmpty() || !this.inFlight.isEmpty();
    }

    int targetCount() {
        return this.fillTargets.size() + this.waterloggedTargets.size();
    }

    boolean hasFillTarget(BlockPos pos) {
        return this.fillTargets.contains(pos);
    }

    boolean hasWaterloggedTarget(BlockPos pos) {
        return this.waterloggedTargets.contains(pos);
    }

    boolean hasWaterloggedTargets() {
        return !this.waterloggedTargets.isEmpty();
    }

    boolean canAttempt(BlockPos pos, long now) {
        return !this.inFlight.containsKey(pos) && this.retryAt.getOrDefault(pos, Long.MIN_VALUE) <= now;
    }

    List<BlockPos> readyWaterloggedTargets(Predicate<BlockPos> reachable) {
        List<BlockPos> ready = new ArrayList<>();
        for (BlockPos pos : this.waterloggedTargets) {
            if (reachable.test(pos)) ready.add(pos);
        }
        return ready;
    }

    List<BlockPos> readyFillTargets(long now, Predicate<BlockPos> isFluid,
                                    Predicate<BlockPos> reachable) {
        List<BlockPos> ready = new ArrayList<>();
        for (BlockPos pos : this.fillTargets) {
            if (this.canAttempt(pos, now) && isFluid.test(pos) && reachable.test(pos)) ready.add(pos);
        }
        return ready;
    }

    boolean canSend(long now, int interval, int limit, boolean globalAvailable) {
        if (limit > 0 && this.sentThisTick >= limit) return false;
        if (this.sentThisTick > 0) return true;
        return (interval <= 0 || this.lastSentTick == Long.MIN_VALUE
                || now - this.lastSentTick >= interval) && globalAvailable;
    }

    void updateTargets(List<PrinterBox> sourceBoxes, Set<BlockPos> discovered,
                       Set<BlockPos> waterlogged, long now, Predicate<BlockPos> isFluid) {
        if (!sourceBoxes.equals(this.scopeBoxes)) {
            this.scopeBoxes = List.copyOf(sourceBoxes);
            this.fillTargets.removeIf(pos -> !this.inFlight.containsKey(pos));
            this.waterloggedTargets.clear();
            this.retryAt.clear();
        }
        this.waterloggedTargets.removeIf(pos -> !waterlogged.contains(pos));
        this.waterloggedTargets.addAll(waterlogged);
        this.fillTargets.removeIf(pos -> !discovered.contains(pos) && !this.inFlight.containsKey(pos));
        discovered.stream().sorted(Comparator.comparingInt(BlockPos::getY)).forEach(this.fillTargets::add);
        this.inFlight.entrySet().removeIf(entry -> !isFluid.test(entry.getKey()) || now - entry.getValue() > 40L);
        this.retryAt.entrySet().removeIf(entry -> !this.fillTargets.contains(entry.getKey()) || entry.getValue() <= now);
    }

    void onBlockUpdated(BlockPos pos, boolean stillFluid) {
        if (this.inFlight.remove(pos) != null && !stillFluid) this.fillTargets.remove(pos);
        this.retryAt.remove(pos);
    }

    void removeFillTarget(BlockPos pos) {
        this.fillTargets.remove(pos);
        this.retryAt.remove(pos);
    }

    void removeWaterloggedTarget(BlockPos pos) {
        this.waterloggedTargets.remove(pos);
    }

    void defer(BlockPos pos, long retryTick) {
        this.retryAt.put(pos.immutable(), retryTick);
    }

    void markInFlight(BlockPos pos, long now, boolean sent) {
        this.inFlight.put(pos.immutable(), now);
        if (sent) {
            this.sentThisTick++;
            this.lastSentTick = now;
        }
    }

    int sentThisTick() {
        return this.sentThisTick;
    }

    long lastSentTick() {
        return this.lastSentTick;
    }
}
