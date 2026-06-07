package me.aleksilassila.litematica.printer.printer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * 主操作队列 + 脏坐标管理。
 * <p>
 * 职责：
 * - 接收来自迭代扫描产生的普通操作（入队尾）
 * - 接收来自 BlockUpdate 监听产生的修复操作（入队首，高优先级）
 * - 支持按坐标替换未执行的操作（同坐标去重）
 * - 管理脏坐标集，每 tick 消费并生成修复操作
 */
public class OperationQueue {
    public static final OperationQueue INSTANCE = new OperationQueue();

    private final LinkedList<QueuedOperation> queue = new LinkedList<>();
    private final Set<BlockPos> dirtyPositions = new HashSet<>();

    private OperationQueue() {}

    public void markDirty(BlockPos pos) {
        dirtyPositions.add(pos);
        RegionTracker.INSTANCE.markDirty(pos);
    }

    /**
     * 消费所有脏坐标并生成修复操作。
     * @return 处理的脏坐标数量
     */
    public int processDirty() {
        if (dirtyPositions.isEmpty()) return 0;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            dirtyPositions.clear();
            return 0;
        }

        if (!SchematicSnapshot.INSTANCE.isSchematicLoaded()) {
            dirtyPositions.clear();
            return 0;
        }

        int count = 0;
        Iterator<BlockPos> it = dirtyPositions.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            it.remove();
            count++;

            BlockState requiredState = SchematicSnapshot.INSTANCE.getRequiredState(pos);
            if (requiredState == null) continue;
            BlockState currentState = level.getBlockState(pos);
            if (requiredState.equals(currentState)) continue;

            replaceOrAdd(pos, true);
        }
        return count;
    }

    public void enqueue(BlockPos pos) {
        replaceOrAdd(pos, false);
    }

    public void enqueueRepair(BlockPos pos) {
        replaceOrAdd(pos, true);
    }

    private void replaceOrAdd(BlockPos pos, boolean isRepair) {
        removePending(pos);

        QueuedOperation op = new QueuedOperation(pos, isRepair);
        if (isRepair) {
            queue.addFirst(op);
        } else {
            queue.addLast(op);
        }
    }

    private void removePending(BlockPos pos) {
        queue.removeIf(op -> op.getPos().equals(pos));
    }

    /**
     * 修复操作在队列中的最大存活 tick 数。
     * 超过此限制的操作会被自动丢弃（可能已被其他handler处理，或无法被任何handler处理）。
     */
    private static final int MAX_QUEUE_AGE = 40; // ~2 秒

    public QueuedOperation peek() {
        return queue.peekFirst();
    }

    public QueuedOperation poll() {
        return queue.pollFirst();
    }

    /**
     * 将操作重新放回队尾（当前 handler 无法处理时，留给下一个 handler）。
     * 增加操作年龄，超过 MAX_QUEUE_AGE 则丢弃。
     * @return true 如果操作被放回队尾，false 如果因过老被丢弃
     */
    public boolean addLast(QueuedOperation op) {
        op.incrementAge();
        if (op.getAge() >= MAX_QUEUE_AGE) {
            return false;
        }
        queue.addLast(op);
        return true;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
        dirtyPositions.clear();
    }
}
