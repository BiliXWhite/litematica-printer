package me.aleksilassila.litematica.printer.handler.scan;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class DirtyRegionTracker {
    public static final int REGION_SIZE = 16;
    private static final int MAX_DIRTY_REGIONS = 8192;
    private static final int HISTORY_COMPACT_THRESHOLD = MAX_DIRTY_REGIONS * 2;

    private static final int XZ_BITS = 22;
    private static final int Y_BITS = 20;
    private static final long XZ_MASK = (1L << XZ_BITS) - 1L;
    private static final long Y_MASK = (1L << Y_BITS) - 1L;

    private final Long2LongOpenHashMap dirtyRegions = new Long2LongOpenHashMap();
    private final ArrayDeque<RegionVersion> regionHistory = new ArrayDeque<>();
    private long version;
    private long historyFloorVersion;

    public DirtyRegionTracker() {
    }

    public synchronized void markDirty(@Nullable BlockPos pos) {
        if (pos == null) {
            return;
        }

        int sectionX = sectionCoord(pos.getX());
        int sectionY = sectionCoord(pos.getY());
        int sectionZ = sectionCoord(pos.getZ());
        this.markDirtyRegion(sectionX, sectionY, sectionZ);

        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;
        if (localX == 0) {
            this.markDirtyRegion(sectionX - 1, sectionY, sectionZ);
        } else if (localX == 15) {
            this.markDirtyRegion(sectionX + 1, sectionY, sectionZ);
        }
        if (localY == 0) {
            this.markDirtyRegion(sectionX, sectionY - 1, sectionZ);
        } else if (localY == 15) {
            this.markDirtyRegion(sectionX, sectionY + 1, sectionZ);
        }
        if (localZ == 0) {
            this.markDirtyRegion(sectionX, sectionY, sectionZ - 1);
        } else if (localZ == 15) {
            this.markDirtyRegion(sectionX, sectionY, sectionZ + 1);
        }
    }

    public synchronized void markChunkDirty(int chunkX, int chunkZ, int minY, int maxY) {
        for (int sectionY = sectionCoord(minY); sectionY <= sectionCoord(maxY); sectionY++) {
            this.markDirtyRegion(chunkX, sectionY, chunkZ);
            this.markDirtyRegion(chunkX - 1, sectionY, chunkZ);
            this.markDirtyRegion(chunkX + 1, sectionY, chunkZ);
            this.markDirtyRegion(chunkX, sectionY, chunkZ - 1);
            this.markDirtyRegion(chunkX, sectionY, chunkZ + 1);
        }
    }

    public synchronized DirtySnapshot snapshotAfter(long lastSeenVersion, @Nullable PrinterBox bounds) {
        long snapshotVersion = this.version;
        if (snapshotVersion <= lastSeenVersion) {
            return new DirtySnapshot(snapshotVersion, List.of());
        }
        if (lastSeenVersion < this.historyFloorVersion) {
            return new DirtySnapshot(snapshotVersion, bounds == null ? List.of() : List.of(bounds));
        }
        List<PrinterBox> boxes = new ArrayList<>();
        for (Long2LongMap.Entry entry : this.dirtyRegions.long2LongEntrySet()) {
            if (entry.getLongValue() <= lastSeenVersion) {
                continue;
            }
            PrinterBox box = this.toBox(entry.getLongKey());
            if (bounds == null || intersects(bounds, box)) {
                boxes.add(box);
            }
        }
        return new DirtySnapshot(snapshotVersion, boxes);
    }

    public synchronized long currentVersion() {
        return this.version;
    }

    public synchronized void clear() {
        this.dirtyRegions.clear();
        this.regionHistory.clear();
        this.version++;
        this.historyFloorVersion = this.version;
    }

    private void markDirtyRegion(int sectionX, int sectionY, int sectionZ) {
        long key = regionKey(sectionX, sectionY, sectionZ);
        long entryVersion = ++this.version;
        this.dirtyRegions.put(key, entryVersion);
        this.regionHistory.addLast(new RegionVersion(key, entryVersion));
        while (this.dirtyRegions.size() > MAX_DIRTY_REGIONS) {
            RegionVersion oldest = this.regionHistory.removeFirst();
            if (this.dirtyRegions.get(oldest.key()) == oldest.version()) {
                this.dirtyRegions.remove(oldest.key());
                this.historyFloorVersion = Math.max(this.historyFloorVersion, oldest.version());
            }
        }
        if (this.regionHistory.size() > HISTORY_COMPACT_THRESHOLD + this.dirtyRegions.size()) {
            this.compactHistory();
        }
    }

    private void compactHistory() {
        List<RegionVersion> regions = new ArrayList<>(this.dirtyRegions.size());
        for (Long2LongMap.Entry entry : this.dirtyRegions.long2LongEntrySet()) {
            regions.add(new RegionVersion(entry.getLongKey(), entry.getLongValue()));
        }
        regions.sort(java.util.Comparator.comparingLong(RegionVersion::version));
        this.regionHistory.clear();
        this.regionHistory.addAll(regions);
    }

    int retainedRegionCount() {
        return this.dirtyRegions.size();
    }

    private PrinterBox toBox(long key) {
        int sectionX = decodeX(key);
        int sectionY = decodeY(key);
        int sectionZ = decodeZ(key);
        int minX = sectionX << 4;
        int minY = sectionY << 4;
        int minZ = sectionZ << 4;
        return new PrinterBox(minX, minY, minZ,
                minX + REGION_SIZE - 1,
                minY + REGION_SIZE - 1,
                minZ + REGION_SIZE - 1);
    }

    private static boolean intersects(PrinterBox first, PrinterBox second) {
        return first.minX <= second.maxX && first.maxX >= second.minX
                && first.minY <= second.maxY && first.maxY >= second.minY
                && first.minZ <= second.maxZ && first.maxZ >= second.minZ;
    }

    private static long regionKey(int sectionX, int sectionY, int sectionZ) {
        return ((long) sectionX & XZ_MASK) << 42
                | ((long) sectionZ & XZ_MASK) << 20
                | ((long) sectionY & Y_MASK);
    }

    private static int decodeX(long key) {
        return signExtend((int) (key >> 42 & XZ_MASK), XZ_BITS);
    }

    private static int decodeZ(long key) {
        return signExtend((int) (key >> 20 & XZ_MASK), XZ_BITS);
    }

    private static int decodeY(long key) {
        return signExtend((int) (key & Y_MASK), Y_BITS);
    }

    private static int signExtend(int value, int bits) {
        int shift = 32 - bits;
        return value << shift >> shift;
    }

    private static int sectionCoord(int blockCoord) {
        return blockCoord >> 4;
    }

    public record DirtySnapshot(long version, List<PrinterBox> boxes) {
    }

    private record RegionVersion(long key, long version) {
    }
}
