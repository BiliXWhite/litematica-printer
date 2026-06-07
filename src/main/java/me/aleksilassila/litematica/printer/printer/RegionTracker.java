package me.aleksilassila.litematica.printer.printer;

import net.minecraft.core.BlockPos;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 子区块脏标志追踪器。
 * 将扫描体积划分为 REGION_SIZE×REGION_SIZE×REGION_SIZE 的子区块，
 * 跟踪哪些子区块有方块变更需要重新迭代扫描。
 *
 * 用于惰性扫描（LAZY）模式下的增量唤醒：
 * - LAZY 时 BlockUpdate 标记对应子区块为 DIRTY
 * - 重新激活后只扫描 DIRTY 子区块（PARTIAL），或全量重扫（FULL）
 */
public class RegionTracker {
    public static final RegionTracker INSTANCE = new RegionTracker();

    public static final int REGION_SIZE = 16;

    private int gridX, gridY, gridZ;
    private int baseX, baseY, baseZ;
    private int totalRegions;
    private BitSet dirty;
    private boolean initialized = false;

    private RegionTracker() {}

    /**
     * 根据 PrinterBox 边界重建子区块网格。
     */
    public synchronized void rebuild(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.baseX = minX;
        this.baseY = minY;
        this.baseZ = minZ;
        this.gridX = Math.max(1, (maxX - minX + REGION_SIZE) / REGION_SIZE);
        this.gridY = Math.max(1, (maxY - minY + REGION_SIZE) / REGION_SIZE);
        this.gridZ = Math.max(1, (maxZ - minZ + REGION_SIZE) / REGION_SIZE);
        this.totalRegions = gridX * gridY * gridZ;
        this.dirty = new BitSet(Math.max(totalRegions, 1));
        this.initialized = true;
    }

    /**
     * 重置（清空网格，进入未初始化状态）。
     */
    public synchronized void reset() {
        this.initialized = false;
        this.dirty = null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 返回方块坐标所在的子区块索引，-1 表示不在网格内。
     */
    public int getRegionIndex(BlockPos pos) {
        int rx = (pos.getX() - baseX) / REGION_SIZE;
        int ry = (pos.getY() - baseY) / REGION_SIZE;
        int rz = (pos.getZ() - baseZ) / REGION_SIZE;
        if (rx < 0 || rx >= gridX || ry < 0 || ry >= gridY || rz < 0 || rz >= gridZ) return -1;
        return (rx * gridY + ry) * gridZ + rz;
    }

    /**
     * 将方块所在的子区块标记为脏。
     */
    public synchronized void markDirty(BlockPos pos) {
        if (!initialized) return;
        int idx = getRegionIndex(pos);
        if (idx >= 0) dirty.set(idx);
    }

    /**
     * 将所有子区块标记为脏（触发全量重扫）。
     */
    public synchronized void markAllDirty() {
        if (!initialized) return;
        dirty.set(0, totalRegions);
    }

    /**
     * 清除指定子区块的脏标志。
     */
    public synchronized void markClean(int regionIndex) {
        if (!initialized || regionIndex < 0 || regionIndex >= totalRegions) return;
        dirty.clear(regionIndex);
    }

    /**
     * 清除所有脏标志（进入 LAZY 模式时调用，避免上次 FULL 扫描积累的脏标志立即唤醒 LAZY）。
     */
    public synchronized void clearAllDirty() {
        if (!initialized) return;
        dirty.clear();
    }

    /**
     * 是否有任何脏子区块。
     */
    public synchronized boolean hasDirtyRegions() {
        return initialized && !dirty.isEmpty();
    }

    /**
     * 脏子区块数量。
     */
    public synchronized int getDirtyCount() {
        return initialized ? dirty.cardinality() : 0;
    }

    /**
     * 获取下一个脏子区块的索引，没有则返回 -1。
     */
    public synchronized int getNextDirtyRegion() {
        if (!initialized) return -1;
        int idx = dirty.nextSetBit(0);
        return idx >= 0 && idx < totalRegions ? idx : -1;
    }

    /**
     * 根据子区块索引构造该子区块对应的 PrinterBox（子扫描体积）。
     */
    public PrinterBox getRegionBox(int regionIndex) {
        if (regionIndex < 0 || regionIndex >= totalRegions) {
            throw new IndexOutOfBoundsException("Region index " + regionIndex + " out of bounds [0, " + totalRegions + ")");
        }
        int rz = regionIndex % gridZ;
        int ry = (regionIndex / gridZ) % gridY;
        int rx = regionIndex / (gridY * gridZ);

        int minX = baseX + rx * REGION_SIZE;
        int minY = baseY + ry * REGION_SIZE;
        int minZ = baseZ + rz * REGION_SIZE;
        int maxX = Math.min(minX + REGION_SIZE - 1, baseX + gridX * REGION_SIZE - 1);
        int maxY = Math.min(minY + REGION_SIZE - 1, baseY + gridY * REGION_SIZE - 1);
        int maxZ = Math.min(minZ + REGION_SIZE - 1, baseZ + gridZ * REGION_SIZE - 1);

        return new PrinterBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 创建遍历所有脏子区块的复合迭代器。
     * 每遍历完一个子区块自动 markClean，用于 PARTIAL 模式。
     */
    public Iterator<BlockPos> createDirtyRegionIterator() {
        return new DirtyRegionIterator();
    }

    private class DirtyRegionIterator implements Iterator<BlockPos> {
        private int currentRegionIdx;
        private PrinterBox currentBox;
        private Iterator<BlockPos> currentIter;
        private boolean exhausted = false;

        DirtyRegionIterator() {
            this.currentRegionIdx = RegionTracker.this.getNextDirtyRegion();
            advanceRegion();
        }

        private void advanceRegion() {
            if (currentRegionIdx < 0) {
                exhausted = true;
                currentIter = null;
                return;
            }
            // 标记上一个已完成的子区块为 clean（只在首次调用时跳过）
            currentBox = RegionTracker.this.getRegionBox(currentRegionIdx);
            currentIter = currentBox.iterator();
        }

        private void ensureIter() {
            while (currentIter != null && !currentIter.hasNext()) {
                // 当前子区块遍历完毕 → 标记 clean
                RegionTracker.this.markClean(currentRegionIdx);

                currentRegionIdx = RegionTracker.this.getNextDirtyRegion();
                if (currentRegionIdx < 0) {
                    exhausted = true;
                    currentIter = null;
                    return;
                }
                currentBox = RegionTracker.this.getRegionBox(currentRegionIdx);
                currentIter = currentBox.iterator();
            }
        }

        @Override
        public boolean hasNext() {
            if (exhausted) return false;
            ensureIter();
            return currentIter != null && currentIter.hasNext();
        }

        @Override
        public BlockPos next() {
            if (!hasNext()) throw new NoSuchElementException();
            return currentIter.next();
        }
    }
}
