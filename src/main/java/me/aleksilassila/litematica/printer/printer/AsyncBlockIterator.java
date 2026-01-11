package me.aleksilassila.litematica.printer.printer;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public final class AsyncBlockIterator {
    private final Queue<BlockPos> resultQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;
    private int valid;

    public AsyncBlockIterator(String name) {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 2);
            return t;
        });
    }

    public void request(MyBox box, Predicate<BlockPos> filter) {
        executor.submit(() -> {
            try {
                int valid = 0;
                resultQueue.clear();
                for (BlockPos pos : box) {
                    if (filter.test(pos)) {
                        if (!resultQueue.contains(pos)) {
                            resultQueue.offer(pos);
                            valid++;
                        }
                    }
                }
                this.valid = valid;
            } catch (Exception ignored) {
            }
        });
    }

    public @Nullable BlockPos poll() {
        return resultQueue.poll();
    }

    public @Nullable BlockPos peek() {
        return resultQueue.peek();
    }

    public Iterator<BlockPos> iterator() {
        return resultQueue.iterator();
    }

    public boolean contains(BlockPos pos) {
        return resultQueue.contains(pos);
    }

    public boolean isEmpty() {
        return resultQueue.isEmpty();
    }

    public int size() {
        return resultQueue.size();
    }

    public void reset() {
        resultQueue.clear();
    }

    public int getValid() {
        return valid;
    }
}
