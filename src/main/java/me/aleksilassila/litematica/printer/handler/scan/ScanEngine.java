package me.aleksilassila.litematica.printer.handler.scan;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.function.Predicate;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;

/**
 * Feature-facing scan middleware.
 *
 * <p>The current implementation is backed by {@link ScanCache}. Keeping this
 * boundary separate lets the scan algorithm evolve without making handlers
 * depend on cache ownership, invalidation storage, or cursor internals.</p>
 */
public final class ScanEngine implements RuntimeComponent {
    private final PrinterRuntime runtime;
    private final ScanCache cache;

    public ScanEngine(PrinterRuntime runtime, ScanCache cache) {
        this.runtime = runtime;
        this.cache = cache;
    }

    public ScanEngine(PrinterRuntime runtime) {
        this(runtime, new ScanCache());
    }

    public void clear() {
        this.cache.clear();
    }

    @Override
    public void onEpochChanged(RuntimeEvent.EpochChanged event) {
        this.clear();
    }

    public void beginTick(ClientLevel level, WorldSchematic schematic, long tickTime) {
        this.cache.beginTick(level, schematic, tickTime, this.runtime.epoch());
    }

    public void invalidate(BlockPos pos) {
        this.cache.invalidate(pos);
    }

    public void invalidateChunk(ClientLevel level, LevelChunk chunk) {
        if (level == null || chunk == null || this.runtime.client().level != level) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        //#if MC >= 260100
        int chunkX = pos.x();
        int chunkZ = pos.z();
        //#else
        //$$ int chunkX = pos.x;
        //$$ int chunkZ = pos.z;
        //#endif
        int minY = level.getMinY();
        this.cache.invalidateChunk(chunkX, chunkZ, minY, minY + level.getHeight() - 1);
    }

    public long dirtyVersion() {
        return this.cache.dirtyVersion();
    }

    public DirtyRegionTracker.DirtySnapshot dirtySnapshotAfter(long lastSeenVersion, PrinterBox bounds) {
        return this.cache.dirtySnapshotAfter(lastSeenVersion, bounds);
    }

    public void resetOwner(String ownerKey) {
        this.cache.resetOwner(ownerKey);
    }

    public ScanCache.ScanMetrics metricsFor(String ownerKey) {
        return this.cache.metricsFor(ownerKey);
    }

    public static long key(BlockPos pos) {
        return ScanCache.key(pos);
    }

    public Iterable<BlockPos> rawIterable(
            String ownerKey,
            PrinterBox sourceBox,
            LocalPlayer player,
            int scanGuardLimit,
            Predicate<BlockPos> preFilter
    ) {
        return this.cache.rawIterable(ownerKey, sourceBox, player, scanGuardLimit, preFilter);
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
        return this.cache.iterable(
                ownerKey,
                sourceBoxes,
                level,
                schematic,
                player,
                scanGuardLimit,
                intent,
                exactPredicate,
                preFilter
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
            Predicate<BlockPos> exactPredicate
    ) {
        return this.cache.iterable(
                ownerKey,
                sourceBoxes,
                level,
                schematic,
                player,
                scanGuardLimit,
                intent,
                exactPredicate
        );
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
        return this.cache.iterable(
                ownerKey,
                sourceBox,
                level,
                schematic,
                player,
                scanGuardLimit,
                intent,
                exactPredicate
        );
    }
}
