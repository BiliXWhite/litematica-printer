package me.aleksilassila.litematica.printer.handler.handlers.print;

import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.aleksilassila.litematica.printer.interaction.StrippableBlockPort;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Owns multi-stage print workflows independently from selection scan progress.
 *
 * <p>Server updates wake only the affected target and its neighbours. A deadline remains as a
 * fallback for missed packets, so a workflow can never depend on player movement or a full scan.</p>
 */
public final class PrintWorkflowScheduler {
    private final LongSupplier tickClock;
    private final StrippableBlockPort strippableBlocks;
    private final Map<BlockPos, PrintTask> tasks = new LinkedHashMap<>();
    private final LongSet readyKeys = new LongOpenHashSet();
    private final LongSet wakeKeys = new LongOpenHashSet();
    private final LongSet materialBlockedKeys = new LongOpenHashSet();
    private long lastRefreshTick = Long.MIN_VALUE;

    public PrintWorkflowScheduler(LongSupplier tickClock, StrippableBlockPort strippableBlocks) {
        this.tickClock = tickClock;
        this.strippableBlocks = strippableBlocks;
    }

    public boolean hasActiveWorkflow() {
        return !this.tasks.isEmpty();
    }

    public boolean hasReadyWorkflow() {
        return !this.readyKeys.isEmpty();
    }

    public void clear() {
        this.tasks.clear();
        this.readyKeys.clear();
        this.wakeKeys.clear();
        this.materialBlockedKeys.clear();
        this.lastRefreshTick = Long.MIN_VALUE;
    }

    public void wake(BlockPos updatedPos) {
        if (updatedPos == null || this.tasks.isEmpty()) return;
        this.wakeKeys.add(updatedPos.asLong());
        for (Direction direction : Direction.values()) {
            this.wakeKeys.add(updatedPos.relative(direction).asLong());
        }
    }

    public void tick(@Nullable ClientLevel level, @Nullable WorldSchematic schematic) {
        long now = this.tickClock.getAsLong();
        if (this.lastRefreshTick == now && this.wakeKeys.isEmpty()) return;
        this.lastRefreshTick = now;
        if (level == null || schematic == null) {
            this.clear();
            return;
        }
        Iterator<PrintTask> iterator = this.tasks.values().iterator();
        while (iterator.hasNext()) {
            PrintTask task = iterator.next();
            long key = task.pos().asLong();
            boolean due = this.wakeKeys.remove(key) || task.nextCheckTick() <= now;
            if (!due) continue;
            if (!task.shouldKeep(level, schematic)) {
                iterator.remove();
                this.readyKeys.remove(key);
                continue;
            }
            this.updateReady(task, level, schematic);
        }
        this.wakeKeys.clear();
    }

    public List<BlockPos> readyTargetPositions() {
        if (this.readyKeys.isEmpty()) return List.of();
        List<BlockPos> positions = new ArrayList<>(this.readyKeys.size());
        for (PrintTask task : this.tasks.values()) {
            if (this.readyKeys.contains(task.pos().asLong())) positions.add(task.pos());
        }
        return positions;
    }

    public boolean isActiveTaskPos(@Nullable BlockPos pos) {
        return pos != null && this.tasks.containsKey(pos);
    }

    public boolean isBreakingTaskPos(@Nullable BlockPos pos) {
        return pos != null && this.tasks.get(pos) instanceof WaterPrintTask water && water.isBreaking();
    }

    public PrintTaskBuildResult buildAction(SchematicBlockContext context) {
        PrintTask task = this.tasks.get(context.blockPos);
        if (task == null) {
            task = PrintTasks.tryCreate(context, this.tickClock, this.strippableBlocks);
            if (task == null) return PrintTaskBuildResult.PASS;
            this.tasks.put(task.pos(), task);
        }
        if (!task.shouldKeep(context.level, context.schematic)) {
            this.remove(task.pos());
            return PrintTaskBuildResult.PASS;
        }
        if (this.materialBlockedKeys.contains(task.pos().asLong())) {
            return PrintTaskBuildResult.SKIP;
        }
        PrintTaskBuildResult result = task.buildAction(context);
        if (!result.hasAction()) {
            this.updateReady(task, context.level, context.schematic);
        }
        return result;
    }

    @Nullable
    public PrintTaskAction createActionHandle(SchematicBlockContext context, Action action) {
        PrintTask task = this.tasks.get(context.blockPos);
        return task == null ? null : task.createActionHandle(context, action);
    }

    public void onActionSuccess(PrintTaskAction actionHandle, SchematicBlockContext context, Action action) {
        actionHandle.onSuccess(context, action);
        this.afterAction(context);
    }

    public void onActionQueued(PrintTaskAction actionHandle, SchematicBlockContext context, Action action) {
        actionHandle.onQueued(context, action);
        this.afterAction(context);
    }

    public void onActionFailure(PrintTaskAction actionHandle, SchematicBlockContext context, Action action) {
        actionHandle.onFailure(context, action);
        this.afterAction(context);
    }

    public void onActionCancelled(PrintTaskAction actionHandle, SchematicBlockContext context, Action action) {
        actionHandle.onCancelled(context, action);
        this.afterAction(context);
    }

    /**
     * Inventory and look synchronization did not submit the stage action. Keep the exact target
     * runnable so it is retried without waiting for the selection scanner to encounter it again.
     */
    public void onActionDeferred(SchematicBlockContext context) {
        PrintTask task = this.tasks.get(context.blockPos);
        if (task == null) return;
        this.updateReady(task, context.level, context.schematic);
    }

    /** Pauses only the affected workflow until an inventory gain wakes material-bound tasks. */
    public void onMaterialUnavailable(SchematicBlockContext context) {
        PrintTask task = this.tasks.get(context.blockPos);
        if (task == null) return;
        long key = task.pos().asLong();
        this.readyKeys.remove(key);
        this.wakeKeys.remove(key);
        this.materialBlockedKeys.add(key);
    }

    public void onInventoryAvailabilityChanged() {
        this.wakeKeys.addAll(this.materialBlockedKeys);
        this.materialBlockedKeys.clear();
    }

    private void afterAction(SchematicBlockContext context) {
        PrintTask task = this.tasks.get(context.blockPos);
        if (task == null) return;
        this.materialBlockedKeys.remove(task.pos().asLong());
        this.readyKeys.remove(task.pos().asLong());
        this.wakeKeys.add(task.pos().asLong());
    }

    private void updateReady(PrintTask task, ClientLevel level, WorldSchematic schematic) {
        long key = task.pos().asLong();
        if (this.materialBlockedKeys.contains(key)
                || task.isWaitingForWorldUpdate(level, schematic)) this.readyKeys.remove(key);
        else this.readyKeys.add(key);
    }

    private void remove(BlockPos pos) {
        this.tasks.remove(pos);
        this.readyKeys.remove(pos.asLong());
        this.wakeKeys.remove(pos.asLong());
        this.materialBlockedKeys.remove(pos.asLong());
    }
}
