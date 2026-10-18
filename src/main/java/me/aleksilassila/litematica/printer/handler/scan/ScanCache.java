package me.aleksilassila.litematica.printer.handler.scan;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class ScanCache {
    private final SchematicBlockIndex schematicIndex = new SchematicBlockIndex();
    private final ScanSessionStore sessions = new ScanSessionStore(this.schematicIndex);
    private final DirtyRegionTracker dirtyRegions;
    private final ScanBudget budget = new ScanBudget();
    private final SectionSnapshotStore snapshots = new SectionSnapshotStore();

    private Object levelIdentity;
    private Object schematicIdentity;
    private RuntimeEpoch runtimeEpoch = RuntimeEpoch.INITIAL;
    private long snapshotRevision;
    private long generationSequence;
    private long tickTime = Long.MIN_VALUE;

    public ScanCache() {
        this(new DirtyRegionTracker());
    }

    public ScanCache(DirtyRegionTracker dirtyRegions) {
        this.dirtyRegions = dirtyRegions;
    }

    public void clear() {
        this.sessions.close();
        this.sessions.clearMetrics();
        this.schematicIndex.clear();
        this.levelIdentity = null;
        this.schematicIdentity = null;
        this.snapshotRevision = 0L;
        this.tickTime = Long.MIN_VALUE;
        this.budget.reset();
        this.snapshots.clear();
        this.dirtyRegions.clear();
    }

    public record ScanMetrics(
            long scanNanos,
            int scannedBlocks,
            int scannedSections,
            int sourceCandidates,
            int acceptedTargets,
            int budgetPauses,
            int completedPasses
    ) {
        private static final ScanMetrics EMPTY = new ScanMetrics(0L, 0, 0, 0, 0, 0, 0);

        static ScanMetrics empty() {
            return EMPTY;
        }

        public boolean hasActivity() {
            return this.scanNanos > 0L
                    || this.scannedBlocks > 0
                    || this.scannedSections > 0
                    || this.sourceCandidates > 0
                    || this.acceptedTargets > 0
                    || this.budgetPauses > 0
                    || this.completedPasses > 0;
        }
    }

    public static long key(BlockPos pos) {
        return key(pos.getX(), pos.getY(), pos.getZ());
    }

    public static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38
                | ((long) z & 0x3FFFFFFL) << 12
                | ((long) y & 0xFFFL);
    }

    public void beginTick(ClientLevel level, WorldSchematic schematic, long tickTime, RuntimeEpoch epoch) {
        if (this.levelIdentity == level && this.schematicIdentity == schematic
                && this.tickTime == tickTime && this.runtimeEpoch.equals(epoch)) {
            return;
        }
        if (!this.runtimeEpoch.equals(epoch) || this.levelIdentity != level || this.schematicIdentity != schematic) {
            this.sessions.close();
            this.sessions.clearMetrics();
            this.schematicIndex.clear();
            this.dirtyRegions.clear();
            this.levelIdentity = level;
            this.schematicIdentity = schematic;
            this.runtimeEpoch = epoch;
            this.snapshotRevision++;
            this.snapshots.clear();
        }
        if (this.tickTime != tickTime) {
            this.sessions.resetMetrics();
        }
        this.tickTime = tickTime;
        this.budget.beginTick(tickTime);
    }

    public ScanMetrics metricsFor(String ownerKey) {
        return this.sessions.metricsFor(normalizeMetricsOwnerKey(ownerKey));
    }

    public void invalidate(BlockPos pos) {
        if (pos == null) {
            return;
        }
        this.snapshotRevision++;
        this.snapshots.invalidateWorld(pos);
        this.dirtyRegions.markDirty(pos);
        this.sessions.invalidate(pos);
    }

    public void invalidateChunk(int chunkX, int chunkZ, int minY, int maxY) {
        if (minY > maxY) {
            return;
        }
        this.snapshotRevision++;
        this.snapshots.invalidateChunk(chunkX, chunkZ, minY, maxY);
        this.dirtyRegions.markChunkDirty(chunkX, chunkZ, minY, maxY);
        this.sessions.invalidateChunk(chunkX, chunkZ);
    }

    public long dirtyVersion() {
        return this.dirtyRegions.currentVersion();
    }

    public DirtyRegionTracker.DirtySnapshot dirtySnapshotAfter(long lastSeenVersion, @Nullable PrinterBox bounds) {
        return this.dirtyRegions.snapshotAfter(lastSeenVersion, bounds);
    }

    public void resetOwner(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            return;
        }
        this.sessions.resetOwner(ownerKey);
    }

    public Iterable<BlockPos> rawIterable(
            String ownerKey,
            PrinterBox sourceBox,
            LocalPlayer player,
            int scanGuardLimit,
            Predicate<BlockPos> preFilter
    ) {
        return this.iterable(ownerKey, List.of(sourceBox), null, null, player, scanGuardLimit,
                ScanIntent.CUSTOM, pos -> true, preFilter, false);
    }

    public Iterable<BlockPos> iterable(
            String ownerKey,
            PrinterBox sourceBox,
            ClientLevel level,
            WorldSchematic schematic,
            LocalPlayer player,
            int scanGuardLimit,
            ScanIntent intent,
            Predicate<BlockPos> exactPredicate
    ) {
        return this.iterable(ownerKey, sourceBox, level, schematic, player, scanGuardLimit, intent, exactPredicate, pos -> true);
    }

    public Iterable<BlockPos> unboundedIterable(
            String ownerKey,
            PrinterBox sourceBox,
            ClientLevel level,
            WorldSchematic schematic,
            LocalPlayer player,
            ScanIntent intent,
            Predicate<BlockPos> exactPredicate,
            Predicate<BlockPos> preFilter
    ) {
        return this.iterable(ownerKey, List.of(sourceBox), level, schematic, player, Integer.MAX_VALUE,
                intent, exactPredicate, preFilter, true);
    }

    public Iterable<BlockPos> iterable(
            String ownerKey,
            PrinterBox sourceBox,
            ClientLevel level,
            WorldSchematic schematic,
            LocalPlayer player,
            int scanGuardLimit,
            ScanIntent intent,
            Predicate<BlockPos> exactPredicate,
            Predicate<BlockPos> preFilter
    ) {
        return this.iterable(ownerKey, List.of(sourceBox), level, schematic, player, scanGuardLimit,
                intent, exactPredicate, preFilter, false);
    }

    public Iterable<BlockPos> iterable(
            String ownerKey,
            List<PrinterBox> sourceBoxes,
            ClientLevel level,
            WorldSchematic schematic,
            LocalPlayer player,
            int scanGuardLimit,
            ScanIntent intent,
            Predicate<BlockPos> exactPredicate
    ) {
        return this.iterable(
                ownerKey,
                sourceBoxes,
                level,
                schematic,
                player,
                scanGuardLimit,
                intent,
                exactPredicate,
                pos -> true
        );
    }

    public Iterable<BlockPos> iterable(
            String ownerKey,
            List<PrinterBox> sourceBoxes,
            ClientLevel level,
            WorldSchematic schematic,
            LocalPlayer player,
            int scanGuardLimit,
            ScanIntent intent,
            Predicate<BlockPos> exactPredicate,
            Predicate<BlockPos> preFilter
    ) {
        if (sourceBoxes == null || sourceBoxes.isEmpty()) {
            return List.of();
        }
        if (intent == ScanIntent.PRINT && !Configs.Print.BREAK_EXTRA_BLOCK.getBooleanValue()) {
            if (this.schematicIndex.ensureBuilt(schematic)) {
                // Existing PRINT sessions may still hold a cursor built from the previous set of
                // loaded schematic chunks. Recreate only this owner so newly loaded chunks cannot
                // be omitted while unrelated scan intents keep their progress.
                this.sessions.resetOwner(ownerKey);
            }
        }
        return this.iterable(ownerKey, sourceBoxes, level, schematic, player, scanGuardLimit,
                intent, exactPredicate, preFilter, false);
    }

    private Iterable<BlockPos> iterable(
            String ownerKey,
            List<PrinterBox> sourceBoxes,
            ClientLevel level,
            WorldSchematic schematic,
            LocalPlayer player,
            int scanGuardLimit,
            ScanIntent intent,
            Predicate<BlockPos> exactPredicate,
            Predicate<BlockPos> preFilter,
            boolean unbounded
    ) {
        String cacheOwnerKey = this.cacheOwnerKey(ownerKey, intent);
        String metricsOwnerKey = normalizeMetricsOwnerKey(ownerKey);
        return new ScanCandidateSourceFactory(
                this.sessions,
                this.budget,
                this.snapshots,
                this.runtimeEpoch,
                () -> this.snapshotRevision,
                () -> ++this.generationSequence,
                this.tickTime
        ).create(
                cacheOwnerKey,
                metricsOwnerKey,
                sourceBoxes,
                level,
                schematic,
                player,
                scanGuardLimit,
                intent,
                exactPredicate,
                preFilter,
                unbounded
        );
    }

    private String cacheOwnerKey(String ownerKey, ScanIntent intent) {
        if (intent == ScanIntent.PRINT && Configs.Print.BREAK_EXTRA_BLOCK.getBooleanValue()) {
            return ownerKey + ":breakExtra";
        }
        return ownerKey;
    }

    private static String normalizeMetricsOwnerKey(String ownerKey) {
        int separator = ownerKey.length();
        int underscore = ownerKey.indexOf('_');
        if (underscore >= 0) {
            separator = Math.min(separator, underscore);
        }
        int colon = ownerKey.indexOf(':');
        if (colon >= 0) {
            separator = Math.min(separator, colon);
        }
        return ownerKey.substring(0, separator);
    }

}
