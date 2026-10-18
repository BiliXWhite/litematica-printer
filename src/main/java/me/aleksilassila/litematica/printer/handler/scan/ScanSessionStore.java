package me.aleksilassila.litematica.printer.handler.scan;

import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/** Owns scan sessions and their per-owner metrics, independent of budget policy. */
final class ScanSessionStore implements AutoCloseable {
    private final Map<String, SectionScanSession> sessions = new HashMap<>();
    private final Map<String, ScanMetricsAccumulator> metrics = new HashMap<>();
    private final SchematicBlockIndex schematicIndex;

    ScanSessionStore(SchematicBlockIndex schematicIndex) {
        this.schematicIndex = schematicIndex;
    }

    ScanMetricsAccumulator metrics(String ownerKey) {
        return this.metrics.computeIfAbsent(ownerKey, ignored -> new ScanMetricsAccumulator());
    }

    ScanCache.ScanMetrics metricsFor(String ownerKey) {
        ScanMetricsAccumulator value = this.metrics.get(ownerKey);
        return value == null ? ScanCache.ScanMetrics.empty() : value.snapshot();
    }

    void resetMetrics() {
        for (ScanMetricsAccumulator value : this.metrics.values()) {
            value.reset();
        }
    }

    void invalidate(BlockPos pos) {
        for (SectionScanSession session : this.sessions.values()) {
            session.invalidate(pos);
        }
    }

    void invalidateChunk(int chunkX, int chunkZ) {
        Iterator<SectionScanSession> iterator = this.sessions.values().iterator();
        while (iterator.hasNext()) {
            SectionScanSession session = iterator.next();
            if (session.intersectsChunkAndNeighbors(chunkX, chunkZ)) {
                session.close();
                iterator.remove();
            }
        }
    }

    void resetOwner(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) return;
        String prefix = ownerKey + ":";
        Iterator<Map.Entry<String, SectionScanSession>> iterator = this.sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SectionScanSession> entry = iterator.next();
            if (entry.getKey().startsWith(prefix)) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    SectionScanSession getOrCreate(
            String ownerKey,
            ScanMetricsAccumulator metrics,
            ScanIntent intent,
            PrinterBox sourceBounds,
            List<PrinterBox> sourceBoxes,
            LocalPlayer player,
            RuntimeEpoch epoch,
            LongSupplier snapshotRevision,
            LongSupplier generationSequence
    ) {
        ScanRegion region = ScanRegion.from(sourceBounds, player);
        String key = ownerKey + ":" + intent.name();
        SectionScanSession session = this.sessions.get(key);
        if (session == null || !session.canReuse(region)) {
            if (session != null) session.close();
            session = new SectionScanSession(
                    region,
                    sourceBoxes,
                    intent,
                    metrics,
                    epoch,
                    snapshotRevision,
                    generationSequence,
                    this.schematicIndex
            );
            this.sessions.put(key, session);
        } else {
            session.updateRegion(region, sourceBoxes);
        }
        return session;
    }

    @Override
    public void close() {
        for (SectionScanSession session : this.sessions.values()) {
            session.close();
        }
        this.sessions.clear();
    }

    void clearMetrics() {
        this.metrics.clear();
    }
}
