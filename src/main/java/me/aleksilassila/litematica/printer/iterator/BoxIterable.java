package me.aleksilassila.litematica.printer.iterator;

import me.aleksilassila.litematica.printer.config.enums.IterationModeType;
import me.aleksilassila.litematica.printer.iterator.impl.BoxIterator;
import me.aleksilassila.litematica.printer.iterator.impl.BoxIteratorCircle;
import me.aleksilassila.litematica.printer.iterator.impl.BoxIteratorLinear;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class BoxIterable implements Iterable<BlockPos> {
    public final IteratorContext context = new IteratorContext();

    private @Nullable IterationModeType mode;
    private @Nullable BoxIterator iterator;

    public boolean update() {
        boolean result = this.context.update();
        if (this.mode != this.context.mode()) {
            this.mode = this.context.mode();
            result = true;
        }
        if (this.iterator != null && iterator.update()) {
            iterator.reset();
            reset(); // 最后清理
            result = true;
        }
        return result;
    }

    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        // 当迭代器模式发生过变化时候, 重新创建并切换到新的迭代器上
        if (this.iterator == null || this.mode != context.mode()) {
            this.mode = this.context.mode();
            this.iterator = createIterator(context.mode());
        }
        return iterator;
    }

    public void reset() {
        this.asyncResultQueue.clear();
        this.asyncResultCount = 0;
        this.iterator = null;
    }

    public static @NotNull BoxIterator createIterator(IterationModeType mode) {
        return switch (mode) {
            case LINEAR -> new BoxIteratorLinear();
            case CIRCLE -> new BoxIteratorCircle();
        };
    }


    // =================== 异步相关的实现 ====================
    public final Queue<BlockPos> asyncResultQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService asyncExecutor;
    private int asyncResultCount;   // 本次任务结果数量

    public BoxIterable() {
        this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BoxIterable-AsyncWorker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 2);
            return t;
        });
    }

    /**
     * 异步提交遍历请求（带过滤条件）
     * @param filter 过滤条件，返回true的BlockPos会被加入结果队列
     */
    public void requestAsync(Predicate<BlockPos> filter) {
        if (!asyncResultQueue.isEmpty()) {
            return;
        }
        asyncExecutor.submit(() -> {
            try {
                asyncResultQueue.clear(); // 清空旧结果
                // 遍历同步迭代器，异步处理
                for (BlockPos pos : this) {
                    if (filter.test(pos)) {
                        asyncResultQueue.offer(pos);
                    }
                }
                asyncResultCount = asyncResultQueue.size();
            } catch (Exception ignored) {
                // 保持和你的代码一致，忽略异常
            }
        });
    }

    /**
     * 异步提交遍历请求（无过滤，全部保留）
     */
    public void requestAsync() {
        requestAsync(pos -> true);
    }

    public int getAsyncResultCount() {
        return asyncResultCount;
    }

    public float getProgress() {
        return (float) asyncResultQueue.size() / (float) asyncResultCount;
    }
}
