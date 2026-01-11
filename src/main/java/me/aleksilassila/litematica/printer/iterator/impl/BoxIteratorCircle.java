package me.aleksilassila.litematica.printer.iterator.impl;

import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import me.aleksilassila.litematica.printer.iterator.IteratorAxis;
import me.aleksilassila.litematica.printer.iterator.IteratorContext;
import net.minecraft.core.BlockPos;

/**
 * 无队列版圆环迭代器
 * 核心：逐坐标实时生成，不缓存队列，避免队列残留导致的卡顿
 */
public class BoxIteratorCircle extends BoxIterator {
    // ========== 迭代核心状态（替代队列） ==========
    // 层/半径状态
    private IteratorAxis layerAxis;
    private IteratorAxis ringA;
    private IteratorAxis ringB;
    private int layerValue;
    private int radius;

    // 当前圆环的遍历状态（关键：替代队列缓存）
    private TraversalSide currentSide; // 当前遍历的边
    private int currentA; // 当前ringA坐标
    private int currentB; // 当前ringB坐标
    private boolean isCurrentRingFinished; // 当前圆环是否遍历完成

    // 圆环范围缓存（避免重复计算）
    private int ringAStart;
    private int ringAEnd;
    private int ringBStart;
    private int ringBEnd;
    private boolean aInc;
    private boolean bInc;

    // 遍历边的枚举
    private enum TraversalSide {
        TOP, RIGHT, BOTTOM, LEFT, NONE
    }

    public BoxIteratorCircle() {
        reset();
    }

    @Override
    public void reset() {
        // 重置层/半径状态
        if (context.order() == null || context.order().axis == null) {
            layerValue = Integer.MIN_VALUE;
            isCurrentRingFinished = true;
            currentSide = TraversalSide.NONE;
            return;
        }

        layerAxis = context.order().axis[0];
        ringA = context.order().axis[1];
        ringB = context.order().axis[2];
        layerValue = layerAxis.getStart(context);
        radius = context.options.circleDirection ? 0 : getMaxRadius();

        // 重置遍历状态
        isCurrentRingFinished = false;
        currentSide = TraversalSide.TOP; // 从“上边”开始遍历
        initCurrentRingRange(); // 初始化当前圆环的范围
    }

    @Override
    public boolean hasNext() {
        // 基础状态校验
        if (layerValue == Integer.MIN_VALUE) {
            return false;
        }

        // 当前圆环还有未遍历的坐标 → 返回true
        if (!isCurrentRingFinished) {
            return true;
        }

        // 当前圆环遍历完，推进到下一个圆环/层
        advanceToNextRing();

        // 推进后仍有有效圆环 → 返回true
        return !isCurrentRingFinished;
    }

    @Override
    public BlockPos next() {
        // 无下一个坐标则返回null
        if (!hasNext()) {
            return null;
        }

        // 生成当前坐标
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        layerAxis.setCoord(pos, layerValue);
        ringA.setCoord(pos, currentA);
        ringB.setCoord(pos, currentB);

        // 推进到当前边的下一个坐标
        boolean hasNextInSide = advanceCurrentSide();

        // 当前边遍历完 → 切换到下一个边
        if (!hasNextInSide) {
            switchToNextSide();
        }

        return pos.immutable();
    }

    /**
     * 推进到当前边的下一个坐标
     * @return 是否还有当前边的下一个坐标
     */
    private boolean advanceCurrentSide() {
        switch (currentSide) {
            case TOP:
                // 上边：遍历ringA，固定ringB
                currentA += aInc ? 1 : -1;
                return aInc ? currentA <= ringAEnd : currentA >= ringAEnd;
            case RIGHT:
                // 右边：遍历ringB，固定ringA
                currentB += bInc ? 1 : -1;
                return bInc ? currentB <= ringBEnd : currentB >= ringBEnd;
            case BOTTOM:
                // 下边：反向遍历ringA，固定ringB
                currentA -= aInc ? 1 : -1;
                return aInc ? currentA >= ringAStart : currentA <= ringAStart;
            case LEFT:
                // 左边：反向遍历ringB，固定ringA
                currentB -= bInc ? 1 : -1;
                return bInc ? currentB > ringBStart : currentB < ringBStart;
            default:
                return false;
        }
    }

    /**
     * 切换到下一个边
     */
    private void switchToNextSide() {
        switch (currentSide) {
            case TOP:
                currentSide = TraversalSide.RIGHT;
                currentA = ringAEnd; // 右边固定ringA为结束值
                currentB = ringBStart + (bInc ? 1 : -1); // 跳过起始值
                break;
            case RIGHT:
                currentSide = TraversalSide.BOTTOM;
                currentB = ringBEnd; // 下边固定ringB为结束值
                currentA = ringAEnd - (aInc ? 1 : -1); // 跳过结束值
                break;
            case BOTTOM:
                currentSide = TraversalSide.LEFT;
                currentA = ringAStart; // 左边固定ringA为起始值
                currentB = ringBEnd - (bInc ? 1 : -1); // 跳过结束值
                break;
            case LEFT:
                // 所有边遍历完 → 当前圆环结束
                isCurrentRingFinished = true;
                currentSide = TraversalSide.NONE;
                break;
            default:
                isCurrentRingFinished = true;
                break;
        }
    }

    /**
     * 推进到下一个圆环/层
     */
    private void advanceToNextRing() {
        int maxRadius = getMaxRadius();

        // 无有效半径 → 直接推进层
        if (maxRadius <= 0) {
            layerValue = nextLayerValue();
            resetCurrentRing();
            return;
        }

        // 推进半径
        if (context.circleDirection()) {
            radius++;
            if (radius > maxRadius) {
                radius = 0;
                layerValue = nextLayerValue();
            }
        } else {
            radius--;
            if (radius < 0) {
                radius = maxRadius;
                layerValue = nextLayerValue();
            }
        }

        // 层值无效 → 迭代结束
        if (layerValue == Integer.MIN_VALUE) {
            isCurrentRingFinished = true;
            return;
        }

        // 初始化新圆环的遍历状态
        resetCurrentRing();
    }

    /**
     * 重置当前圆环的遍历状态（初始化范围+边）
     */
    private void resetCurrentRing() {
        // 层值无效 → 标记为结束
        if (layerValue == Integer.MIN_VALUE) {
            isCurrentRingFinished = true;
            currentSide = TraversalSide.NONE;
            return;
        }

        // 初始化当前圆环的范围
        initCurrentRingRange();

        // 范围无效 → 标记为结束
        if (ringAStart > ringAEnd || ringBStart > ringBEnd) {
            isCurrentRingFinished = true;
            currentSide = TraversalSide.NONE;
            return;
        }

        // 重置遍历状态：从“上边”开始
        isCurrentRingFinished = false;
        currentSide = TraversalSide.TOP;
        currentA = aInc ? ringAStart : ringAEnd; // 上边起始A值
        currentB = bInc ? ringBStart : ringBEnd; // 上边固定B值
    }

    /**
     * 初始化当前圆环的范围和迭代方向
     */
    private void initCurrentRingRange() {
        int maxRadius = getMaxRadius();
        if (radius < 0 || radius > maxRadius) {
            ringAStart = ringAEnd = ringBStart = ringBEnd = 0;
            aInc = bInc = true;
            return;
        }

        // 计算圆环范围
        ringAStart = ringA.getMin(context) + radius;
        ringAEnd = ringA.getMax(context) - radius;
        ringBStart = ringB.getMin(context) + radius;
        ringBEnd = ringB.getMax(context) - radius;

        // 迭代方向
        aInc = ringA.isIncrement(context);
        bInc = ringB.isIncrement(context);

        // 范围修正（确保合法）
        ringAStart = Math.max(ringAStart, ringA.getMin(context));
        ringAEnd = Math.min(ringAEnd, ringA.getMax(context));
        ringBStart = Math.max(ringBStart, ringB.getMin(context));
        ringBEnd = Math.min(ringBEnd, ringB.getMax(context));
    }

    /**
     * 获取下一层值
     */
    private int nextLayerValue() {
        if (layerValue == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        int next = layerValue + (layerAxis.isIncrement(context) ? 1 : -1);
        return layerAxis.isOutOfBound(context, next) ? Integer.MIN_VALUE : next;
    }

    /**
     * 获取最大半径（确保≥1）
     */
    private int getMaxRadius() {
        if (ringA == null || ringB == null || context == null) {
            return 1;
        }
        int a = (ringA.getMax(context) - ringA.getMin(context)) / 2;
        int b = (ringB.getMax(context) - ringB.getMin(context)) / 2;
        return Math.max(1, Math.min(a, b));
    }
}
