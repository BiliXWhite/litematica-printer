package me.aleksilassila.litematica.printer.handler.scan;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.core.scan.ScanGeneration;
import me.aleksilassila.litematica.printer.core.scan.ScanHandle;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.LongSupplier;

/**
 * Owns one resumable, player-distance ordered scan pass.
 *
 * <p>The cache decides when a session may run and how much time it receives. This class only
 * owns traversal progress, invalidated positions and live world/schematic classification.</p>
 */
final class SectionScanSession {
    private static final int BUDGET_CHECK_INTERVAL = 8;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ScanIntent intent;
    private final ScanMetricsAccumulator metrics;
    private ScanRegion region;
    private List<PrinterBox> sourceBoxes;
    private PositionCursor distanceCursor;
    private final RuntimeEpoch epoch;
    private final LongSupplier snapshotRevision;
    private final LongSupplier generationSequence;
    private final SchematicBlockIndex schematicIndex;
    private long sourceRevision;
    private long cursorRevision;
    private long exhaustedUntilTick = Long.MIN_VALUE;
    private final PriorityQueue<ScanDirtyPosition> dirtyPositions = new PriorityQueue<>();
    private final LongSet dirtyPositionKeys = new LongOpenHashSet();
    private boolean paused;
    private boolean closed;
    private ScanHandle scanHandle;
    private final BlockPos.MutableBlockPos liveMutable = new BlockPos.MutableBlockPos();
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private boolean lastChunkLoaded;
    private final ChunkCandidateCache chunkCandidateCache = new ChunkCandidateCache();

    SectionScanSession(
            ScanRegion region,
            List<PrinterBox> sourceBoxes,
            ScanIntent intent,
            ScanMetricsAccumulator metrics,
            RuntimeEpoch epoch,
            LongSupplier snapshotRevision,
            LongSupplier generationSequence,
            SchematicBlockIndex schematicIndex
    ) {
        this.region = region;
        this.sourceBoxes = List.copyOf(sourceBoxes);
        this.intent = intent;
        this.metrics = metrics;
        this.epoch = epoch;
        this.snapshotRevision = snapshotRevision;
        this.generationSequence = generationSequence;
        this.schematicIndex = schematicIndex;
        this.scanHandle = this.createScanHandle();
        this.distanceCursor = this.createDistanceCursor();
    }

    SectionScanSession(
            ScanRegion region,
            List<PrinterBox> sourceBoxes,
            ScanIntent intent,
            ScanMetricsAccumulator metrics
    ) {
        this(region, sourceBoxes, intent, metrics, RuntimeEpoch.INITIAL, () -> 0L, () -> 0L, null);
    }

    boolean canReuse(ScanRegion region) {
        return this.region.sameSectionWindow(region)
                && this.region.maxDistanceBand() == region.maxDistanceBand();
    }

    void updateRegion(ScanRegion region, List<PrinterBox> sourceBoxes) {
        boolean boxesChanged = !this.sourceBoxes.equals(sourceBoxes);
        // Compare the section window (16-block granularity), not the exact center. The center
        // follows the player's block position; bumping the revision for every single block of
        // movement made finishPass() rebuild the cursor from scratch on every pass, which
        // showed up as a permanent full rescan in the HUD. Within a section the in-flight
        // cursor is reused and fine center movement only shifts the reach shape slightly.
        boolean windowChanged = !this.region.sameSectionWindow(region);
        this.region = region;
        if (boxesChanged) {
            this.sourceBoxes = List.copyOf(sourceBoxes);
        }
        if (boxesChanged || windowChanged) {
            this.sourceRevision++;
        }
        // Keep the active cursor while the section window remains stable. Rebuilding it for every
        // block of player movement repeatedly walks the already-scanned prefix and starves the
        // edge of large ranges. A revision check at the end of this pass schedules exactly one
        // follow-up pass over the latest center and boxes before completion is reported.
    }

    boolean canScan(long tickTime) {
        if (this.closed) {
            return false;
        }
        if (this.exhaustedUntilTick == Long.MIN_VALUE) {
            return true;
        }
        if (tickTime < this.exhaustedUntilTick) {
            return false;
        }
        this.resetProgress();
        return true;
    }

    private void resetProgress() {
        this.rebuildDistanceCursor();
        this.dirtyPositions.clear();
        this.dirtyPositionKeys.clear();
    }

    private void rebuildDistanceCursor() {
        this.distanceCursor.close();
        this.scanHandle.close();
        this.scanHandle = this.createScanHandle();
        this.distanceCursor = this.createDistanceCursor();
        this.cursorRevision = this.sourceRevision;
        this.exhaustedUntilTick = Long.MIN_VALUE;
        this.lastChunkX = Integer.MIN_VALUE;
        this.lastChunkZ = Integer.MIN_VALUE;
        this.lastChunkLoaded = false;
    }

    private PositionCursor createDistanceCursor() {
        if (this.intent == ScanIntent.PRINT
                && !Configs.Print.BREAK_EXTRA_BLOCK.getBooleanValue()
                && this.schematicIndex != null
                && this.schematicIndex.isReady()) {
            return new IndexedPositionCursor(this.schematicIndex, this.sourceBoxes, this.region);
        }
        return new SynchronousPositionCursor(
                this.sourceBoxes,
                this.region.centerX(),
                this.region.centerY(),
                this.region.centerZ(),
                this.region.maxDistanceBand()
        );
    }

    boolean hasPendingSource(long tickTime) {
        return this.canScan(tickTime)
                && (!this.dirtyPositions.isEmpty() || !this.distanceCursor.isComplete());
    }

    boolean wasPaused() {
        return this.paused;
    }

    boolean belongsTo(RuntimeEpoch epoch) {
        return !this.closed && this.epoch.equals(epoch) && this.scanHandle.accepts(this.scanHandle.generation());
    }

    boolean contains(BlockPos pos) {
        return ScanGeometry.containsAny(this.sourceBoxes, pos);
    }

    boolean intersectsChunkAndNeighbors(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        for (PrinterBox box : this.sourceBoxes) {
            boolean overlapsX = box.minX <= maxX && box.maxX >= minX;
            boolean overlapsZ = box.minZ <= maxZ && box.maxZ >= minZ;
            if ((overlapsX && box.minZ <= maxZ + 1 && box.maxZ >= minZ - 1)
                    || (overlapsZ && box.minX <= maxX + 1 && box.maxX >= minX - 1)) {
                return true;
            }
        }
        return false;
    }

    void invalidate(BlockPos pos) {
        this.chunkCandidateCache.invalidate(
                ScanGeometry.sectionCoord(pos.getX()),
                ScanGeometry.sectionCoord(pos.getZ())
        );
        this.addDirtyPosition(pos);
        if (this.intent == ScanIntent.FILL
                || this.intent == ScanIntent.PRINT
                || this.intent == ScanIntent.BEDROCK) {
            // Support, waterlogging and bedrock-machine exposure are neighbor-dependent.
            // Revisit exactly the six affected targets rather than restarting the selection.
            // Fluid does not need this expansion because flowing liquid already produces the
            // neighboring updates it needs.
            for (Direction direction : DIRECTIONS) {
                this.addDirtyPosition(pos.relative(direction));
            }
        }
        this.paused = false;
    }

    private void addDirtyPosition(BlockPos pos) {
        if (pos == null || !ScanGeometry.containsAny(this.sourceBoxes, pos)) {
            return;
        }
        this.exhaustedUntilTick = Long.MIN_VALUE;
        long key = ScanCache.key(pos);
        if (this.dirtyPositionKeys.add(key)) {
            this.dirtyPositions.add(new ScanDirtyPosition(
                    pos.immutable(),
                    ScanGeometry.distanceSqr(pos, this.region.centerX(), this.region.centerY(), this.region.centerZ())
            ));
        }
    }

    Candidate next(
            WorldObservationPort observation,
            long tickTime,
            BooleanSupplier shouldPause,
            Predicate<BlockPos> preFilter,
            boolean unbounded
    ) {
        this.paused = false;
        if (!this.canScan(tickTime)) {
            return null;
        }
        return this.nextByPlayerDistance(observation, tickTime, shouldPause, preFilter, unbounded);
    }

    private Candidate nextByPlayerDistance(
            WorldObservationPort observation,
            long tickTime,
            BooleanSupplier shouldPause,
            Predicate<BlockPos> preFilter,
            boolean unbounded
    ) {
        if (observation == null) {
            this.cursorRevision = this.sourceRevision;
            this.finishPass(tickTime);
            return null;
        }
        int scanned = 0;
        while (true) {
            if (!unbounded
                    && scanned > 0
                    && scanned % BUDGET_CHECK_INTERVAL == 0
                    && shouldPause.getAsBoolean()) {
                this.paused = true;
                return null;
            }
            BlockPos dirtyPos = this.pollDirtyPositionBefore(this.distanceCursor.peekDistanceSqr());
            int x;
            int y;
            int z;
            if (dirtyPos != null) {
                x = dirtyPos.getX();
                y = dirtyPos.getY();
                z = dirtyPos.getZ();
                this.liveMutable.set(x, y, z);
            } else {
                PositionCursor.PollResult pollResult = this.distanceCursor.poll(this.liveMutable);
                if (pollResult == PositionCursor.PollResult.COMPLETE) {
                    if (this.finishPass(tickTime)) {
                        return null;
                    }
                    continue;
                }
                x = this.liveMutable.getX();
                y = this.liveMutable.getY();
                z = this.liveMutable.getZ();
            }
            scanned++;

            if (!preFilter.test(this.liveMutable)) {
                continue;
            }

            int chunkX = ScanGeometry.sectionCoord(x);
            int chunkZ = ScanGeometry.sectionCoord(z);
            if (chunkX != this.lastChunkX || chunkZ != this.lastChunkZ) {
                this.lastChunkX = chunkX;
                this.lastChunkZ = chunkZ;
                if (!observation.hasChunk(chunkX, chunkZ)) {
                    this.lastChunkLoaded = false;
                } else {
                    this.lastChunkLoaded = this.chunkCandidateCache.getOrCompute(
                            chunkX,
                            chunkZ,
                            () -> observation.hasCandidatesInChunk(
                                this.intent,
                                chunkX,
                                chunkZ,
                                Configs.Print.BREAK_EXTRA_BLOCK.getBooleanValue()
                            )
                    );
                }
            }
            if (!this.lastChunkLoaded) {
                continue;
            }

            this.metrics.recordScannedSection(ScanGeometry.sectionKey(
                    chunkX, ScanGeometry.sectionCoord(y), chunkZ));

            this.metrics.scannedBlocks++;
            if (this.intent == ScanIntent.CUSTOM) {
                return new Candidate(new BlockPos(x, y, z), (byte) 0);
            }
            byte flags = observation.classify(
                    this.intent,
                    this.liveMutable,
                    Configs.Print.BREAK_EXTRA_BLOCK.getBooleanValue()
            );
            if (flags != 0) {
                return new Candidate(new BlockPos(x, y, z), flags);
            }
        }
    }

    private BlockPos pollDirtyPositionBefore(long sourceDistanceSqr) {
        while (!this.dirtyPositions.isEmpty()) {
            ScanDirtyPosition dirty = this.dirtyPositions.peek();
            if (dirty.distanceSqr() > sourceDistanceSqr) {
                return null;
            }
            this.dirtyPositions.poll();
            BlockPos pos = dirty.pos();
            this.dirtyPositionKeys.remove(ScanCache.key(pos));
            if (ScanGeometry.containsAny(this.sourceBoxes, pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean finishPass(long tickTime) {
        if (this.cursorRevision != this.sourceRevision) {
            this.rebuildDistanceCursor();
            return false;
        }
        // Restart on the next client tick. Lazy admission is owned by the feature runtime and must count
        // real empty passes instead of coupling availability to the lazy admission window.
        this.exhaustedUntilTick = tickTime + 1;
        this.metrics.completedPasses++;
        return true;
    }

    void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.scanHandle.close();
        this.distanceCursor.close();
        this.dirtyPositions.clear();
        this.dirtyPositionKeys.clear();
        this.chunkCandidateCache.clear();
    }

    private ScanHandle createScanHandle() {
        return new ScanHandle(new ScanGeneration(
                this.epoch,
                this.sourceRevision,
                this.snapshotRevision.getAsLong(),
                this.generationSequence.getAsLong()
        ));
    }

    record Candidate(BlockPos pos, byte flags) {
        boolean acceptedByFlags(ScanIntent intent) {
            return intent.acceptsByFlags(this.flags);
        }
    }

}
