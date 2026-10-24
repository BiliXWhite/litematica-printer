package me.aleksilassila.litematica.printer.search;

import me.aleksilassila.litematica.printer.enums.IterationOrderType;
import me.aleksilassila.litematica.printer.enums.RadiusShapeType;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// 全模块共用的异步搜索管线：主线程提交任务，工作线程扫描，主线程从队列消费
public final class SearchScheduler {
    public static final SearchScheduler INSTANCE = new SearchScheduler();

    private static final int TILE_EDGE = 16;

    private final ExecutorService scheduler =
            Executors.newSingleThreadExecutor(daemon("Printer-Search-Scheduler"));
    private final Map<String, PositionQueue> queues = new ConcurrentHashMap<>();
    private final AtomicBoolean busy = new AtomicBoolean();

    private ExecutorService workers;
    private int workerCount;

    private SearchScheduler() {
    }

    public boolean isBusy() {
        return busy.get();
    }

    @Nullable
    public BlockPos poll(String moduleId) {
        PositionQueue queue = queues.get(moduleId);
        return queue == null ? null : queue.poll();
    }

    public void clear(String moduleId) {
        PositionQueue queue = queues.get(moduleId);
        if (queue != null) queue.clear();
    }

    // 工作池仍忙时返回 false，本轮不排队
    public boolean submit(List<SearchSpec> specs, int threads) {
        if (specs == null || specs.isEmpty() || !busy.compareAndSet(false, true)) return false;

        List<Tile> tiles = new ArrayList<>();
        for (SearchSpec spec : specs) addTiles(spec, tiles);
        if (tiles.isEmpty()) {
            busy.set(false);
            return false;
        }

        int count = Math.max(1, threads);
        scheduler.execute(() -> runRound(tiles, count));
        return true;
    }

    private void runRound(List<Tile> tiles, int threads) {
        try {
            ensureWorkers(threads);
            // 分批提交并顺序发布，既限制并发也保持原有迭代顺序
            for (int start = 0; start < tiles.size(); start += threads) {
                int end = Math.min(start + threads, tiles.size());
                List<Future<List<BlockPos>>> futures = new ArrayList<>(end - start);
                for (int i = start; i < end; i++) {
                    Tile tile = tiles.get(i);
                    futures.add(workers.submit(() -> scan(tile)));
                }
                for (int i = start; i < end; i++) {
                    List<BlockPos> result;
                    try {
                        result = futures.get(i - start).get();
                    } catch (Throwable throwable) {
                        result = List.of();
                    }
                    publish(tiles.get(i).spec().moduleId(), result);
                }
            }
        } catch (Throwable throwable) {
            // 本轮中断，等待下一次提交
        } finally {
            busy.set(false);
        }
    }

    private void ensureWorkers(int count) {
        if (workers != null && workerCount == count) return;
        if (workers != null) workers.shutdown();
        workerCount = count;
        workers = Executors.newFixedThreadPool(count, daemon("Printer-Search-Worker"));
    }

    private void publish(String moduleId, List<BlockPos> positions) {
        PositionQueue queue = queues.computeIfAbsent(moduleId, ignored -> new PositionQueue());
        for (BlockPos pos : positions) queue.offer(pos);
    }

    private static List<BlockPos> scan(Tile tile) {
        SearchSpec spec = tile.spec();
        ScanTarget target = spec.target();
        @Nullable Vec3 eyePos = spec.eyePos();
        RadiusShapeType shape = spec.shape();
        double range = spec.range();

        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : tile.toBox()) {
            if (eyePos != null && !PlayerUtils.canInteracted(pos, eyePos, range, shape)) continue;

            boolean inWorkspace;
            try {
                inWorkspace = target.isInWorkspace(pos);
            } catch (Throwable throwable) {
                inWorkspace = true;
            }
            if (!inWorkspace) continue;

            boolean candidate;
            try {
                candidate = target.isCandidatePos(pos);
            } catch (Throwable throwable) {
                candidate = false;
            }
            if (candidate) found.add(pos.immutable());
        }
        return found;
    }

    private static void addTiles(SearchSpec spec, List<Tile> out) {
        PrinterBox box = spec.box();
        int[] min = {box.minX, box.minY, box.minZ};
        int[] max = {box.maxX, box.maxY, box.maxZ};
        boolean[] inc = {box.xIncrement, box.yIncrement, box.zIncrement};

        IterationOrderType.Axis[] order = box.iterationMode.axis;
        int[] orderIdx = {axisIndex(order[0]), axisIndex(order[1]), axisIndex(order[2])};

        int[][] origins = new int[3][];
        for (int axis = 0; axis < 3; axis++) {
            origins[axis] = originSequence(min[axis], max[axis], inc[axis]);
        }

        for (int o0 : origins[orderIdx[0]]) {
            for (int o1 : origins[orderIdx[1]]) {
                for (int o2 : origins[orderIdx[2]]) {
                    int[] origin = new int[3];
                    origin[orderIdx[0]] = o0;
                    origin[orderIdx[1]] = o1;
                    origin[orderIdx[2]] = o2;

                    int[] tileMin = new int[3];
                    int[] tileMax = new int[3];
                    for (int axis = 0; axis < 3; axis++) {
                        if (inc[axis]) {
                            tileMin[axis] = origin[axis];
                            tileMax[axis] = Math.min(origin[axis] + TILE_EDGE - 1, max[axis]);
                        } else {
                            tileMax[axis] = origin[axis];
                            tileMin[axis] = Math.max(origin[axis] - TILE_EDGE + 1, min[axis]);
                        }
                    }
                    out.add(new Tile(spec,
                            tileMin[0], tileMin[1], tileMin[2],
                            tileMax[0], tileMax[1], tileMax[2]));
                }
            }
        }
    }

    private static int[] originSequence(int min, int max, boolean increment) {
        if (max < min) return new int[0];
        int count = (max - min) / TILE_EDGE + 1;
        int[] seq = new int[count];
        if (increment) {
            for (int i = 0; i < count; i++) seq[i] = min + i * TILE_EDGE;
        } else {
            for (int i = 0; i < count; i++) seq[i] = max - i * TILE_EDGE;
        }
        return seq;
    }

    private static int axisIndex(IterationOrderType.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static ThreadFactory daemon(String name) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private record Tile(
            SearchSpec spec,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    ) {
        PrinterBox toBox() {
            PrinterBox tileBox = new PrinterBox(minX, minY, minZ, maxX, maxY, maxZ);
            PrinterBox source = spec.box();
            tileBox.iterationMode = source.iterationMode;
            tileBox.xIncrement = source.xIncrement;
            tileBox.yIncrement = source.yIncrement;
            tileBox.zIncrement = source.zIncrement;
            return tileBox;
        }
    }

    // 有界、按坐标去重、线程安全
    private static final class PositionQueue {
        private static final int CAPACITY = 10_000;

        private final ConcurrentLinkedQueue<BlockPos> queue = new ConcurrentLinkedQueue<>();
        private final java.util.Set<BlockPos> present = ConcurrentHashMap.newKeySet();
        private final AtomicInteger size = new AtomicInteger();

        // 重复或队列已满时返回 false
        boolean offer(BlockPos pos) {
            if (pos == null) return false;
            BlockPos immutable = pos.immutable();
            if (!present.add(immutable)) return false;
            if (size.get() >= CAPACITY) {
                present.remove(immutable);
                return false;
            }
            queue.add(immutable);
            size.incrementAndGet();
            return true;
        }

        @Nullable
        BlockPos poll() {
            BlockPos pos = queue.poll();
            if (pos == null) return null;
            present.remove(pos);
            size.decrementAndGet();
            return pos;
        }

        void clear() {
            queue.clear();
            present.clear();
            size.set(0);
        }
    }
}
