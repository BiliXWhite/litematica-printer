package me.aleksilassila.litematica.printer.iterator.impl;

import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import me.aleksilassila.litematica.printer.iterator.IteratorAxis;
import net.minecraft.core.BlockPos;

public class BoxIteratorLinear extends BoxIterator {
    private final BlockPos.MutableBlockPos currPos = new BlockPos.MutableBlockPos();
    private boolean isFirst = true;

    // 重置线性迭代器状态
    @Override
    public void reset() {
        isFirst = true;
        currPos.set(BlockPos.ZERO); // 清空当前位置
    }

    @Override
    public boolean hasNext() {
        if (isFirst) return true;

        // 从上下文获取迭代顺序
        IterationOrderType order = context.order();
        IteratorAxis[] axis = order.axis;

        // 检查是否到达所有轴的目标边界
        boolean isAllTarget = axis[0].getCoord(currPos) == axis[0].getTarget(context)
                && axis[1].getCoord(currPos) == axis[1].getTarget(context)
                && axis[2].getCoord(currPos) == axis[2].getTarget(context);

        if (isAllTarget) {
            reset(); // 迭代完成，重置状态
            return false;
        }
        return true;
    }

    @Override
    public BlockPos next() {
        if (isFirst) {
            // 初始化起始位置：从上下文获取各轴起始值
            isFirst = false;
            currPos.set(
                    IteratorAxis.X.getStart(context),
                    IteratorAxis.Y.getStart(context),
                    IteratorAxis.Z.getStart(context)
            );
            return currPos.immutable();
        }

        // 从上下文获取迭代优先级顺序
        IterationOrderType order = context.order();
        IteratorAxis[] priority = order.axis;

        // 迭代第一优先级轴
        if (!incrementAxis(priority[0])) {
            // 第一轴到边界，重置并迭代第二轴
            resetAxis(priority[0]);
            if (!incrementAxis(priority[1])) {
                // 第二轴到边界，重置并迭代第三轴
                resetAxis(priority[1]);
                incrementAxis(priority[2]);
            }
        }

        return currPos.immutable();
    }

    /**
     * 递增指定轴坐标
     */
    private boolean incrementAxis(IteratorAxis iteratorAxis) {
        int current = iteratorAxis.getCoord(currPos);
        int next = iteratorAxis.nextCoord(context, current);
        if (iteratorAxis.isOutOfBound(context, next)) {
            return false;
        }
        iteratorAxis.setCoord(currPos, next);
        return true;
    }

    /**
     * 重置指定轴到起始边界
     */
    private void resetAxis(IteratorAxis iteratorAxis) {
        iteratorAxis.setCoord(currPos, iteratorAxis.getStart(context));
    }
}