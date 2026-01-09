package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.config.enums.IterationModeType;
import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 纯整数版包围盒，适配BlockPos整数坐标，支持线性/圆环迭代，全轴遵循Axis规则
 */
public class MyBox implements Iterable<BlockPos> {
    // 整数版边界（核心：无浮点）
    public final int minX;
    public final int maxX;
    public final int minY;
    public final int maxY;
    public final int minZ;
    public final int maxZ;

    // 各轴迭代方向（true=递增，false=递减）
    private boolean yIncrement = true;
    private boolean xIncrement = true;
    private boolean zIncrement = true;

    private static Iterator<BlockPos> circleIterator;
    private static Iterator<BlockPos> linearIterator;
    private IterationOrderType iterationMode = IterationOrderType.XZY;
    private IterationModeType iterationType = IterationModeType.LINEAR;
    private BlockPos centerPos; // 玩家中心坐标（整数）
    private boolean circleFromOutside = true; // 圆环迭代方向：true=外向内，false=内向外

    public MyBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // 确保min <= max（自动修正）
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public MyBox(BlockPos pos1, BlockPos pos2) {
        this(
                pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX(), pos2.getY(), pos2.getZ()
        );
    }

    public MyBox(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX
                && y >= this.minY && y <= this.maxY
                && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public MyBox expand(int x, int y, int z) {
        return new MyBox(
                this.minX - x, this.minY - y, this.minZ - z,
                this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    public MyBox expand(int value) {
        return this.expand(value, value, value);
    }

    public void setYIncrement(boolean yIncrement) {
        if (this.yIncrement != yIncrement) {
            resetIterations();
            this.yIncrement = yIncrement;
        }
    }

    public void setXIncrement(boolean xIncrement) {
        if (this.xIncrement != xIncrement) {
            resetIterations();
            this.xIncrement = xIncrement;
        }
    }

    public void setZIncrement(boolean zIncrement) {
        if (this.zIncrement != zIncrement) {
            resetIterations();
            this.zIncrement = zIncrement;
        }
    }

    public void setCircleDirection(boolean fromOutside) {
        if (fromOutside != this.circleFromOutside) {
            resetIterations();
            this.circleFromOutside = fromOutside;
        }
    }

    public void setIterationModeType(IterationModeType type) {
        if (type != this.iterationType) {
            resetIterations();
            this.iterationType = type;
        }
    }

    public void setIterationOrderType(IterationOrderType mode) {
        this.iterationMode = mode;
    }

    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        if (circleIterator == null) {
            circleIterator = createCircleIterator();
        }
        if (linearIterator == null) {
            linearIterator = createLinearIterator();
        }
        if (iterationType == IterationModeType.CIRCLE) {
            return circleIterator;
        } else {
            return linearIterator;
        }
    }

    public void resetCircleIterator() {
        if (circleIterator != null) {
            circleIterator = null;
        }
    }

    public void resetLinearIterator() {
        if (linearIterator != null) {
            linearIterator = null;
        }
    }

    public void resetIterations() {
        resetCircleIterator();
        resetLinearIterator();
    }

    private Iterator<BlockPos> createLinearIterator() {
        return new Iterator<>() {
            private final BlockPos.MutableBlockPos currPos = new BlockPos.MutableBlockPos();
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                if (isFirst) return true;

                // 检查是否到达所有轴的目标边界
                Axis[] axis = iterationMode.axis;

                boolean b = !(axis[0].getCoord(currPos) == axis[0].getTarget(MyBox.this)
                        && axis[1].getCoord(currPos) == axis[1].getTarget(MyBox.this)
                        && axis[2].getCoord(currPos) == axis[2].getTarget(MyBox.this));

                if (!b) {
                    resetLinearIterator();
                    return false;
                }
                return true;
            }

            @Override
            public BlockPos next() {
                if (isFirst) {
                    // 初始化起始位置（整数）
                    isFirst = false;
                    currPos.set(
                            Axis.X.getStart(MyBox.this),
                            Axis.Y.getStart(MyBox.this),
                            Axis.Z.getStart(MyBox.this)
                    );
                    return currPos.immutable();
                }

                Axis[] priority = iterationMode.axis;
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
             * 递增指定轴坐标（整数）
             */
            private boolean incrementAxis(Axis axis) {
                int current = axis.getCoord(currPos);
                int next = axis.nextCoord(MyBox.this, current);

                if (axis.isOutOfBound(MyBox.this, next)) {
                    return false;
                }

                axis.setCoord(currPos, next);
                return true;
            }

            /**
             * 重置指定轴到起始边界（整数）
             */
            private void resetAxis(Axis axis) {
                axis.setCoord(currPos, axis.getStart(MyBox.this));
            }
        };
    }

    private Iterator<BlockPos> createCircleIterator() {
        return new Iterator<>() {
            private final Queue<BlockPos> circleQueue = new LinkedList<>();
            private final BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();

            // 初始化：生成所有圆环层的坐标（全整数）
            {
                if (centerPos == null) {
                    // 默认中心为包围盒整数中心点
                    centerPos = new BlockPos(
                            (minX + maxX) / 2,
                            (minY + maxY) / 2,
                            (minZ + maxZ) / 2
                    );
                }

                // 计算最大半径（整数，包围盒到中心的最远距离）
                int maxRadius = Math.max(
                        Math.max(Math.abs(maxX - centerPos.getX()), Math.abs(maxY - centerPos.getY())),
                        Math.abs(maxZ - centerPos.getZ())
                );

                // 按迭代方向生成半径列表（外向内/内向外）
                int[] radii = new int[maxRadius + 1];
                if (circleFromOutside) {
                    for (int i = 0; i <= maxRadius; i++) {
                        radii[i] = maxRadius - i;
                    }
                } else {
                    for (int i = 0; i <= maxRadius; i++) {
                        radii[i] = i;
                    }
                }

                // 遍历每个半径层
                for (int radius : radii) {
                    if (radius < 0) continue;

                    // 1. 按Axis.X规则生成X轴遍历范围（整数）
                    int xStart = centerPos.getX() - radius;
                    int xEnd = centerPos.getX() + radius;
                    int[] xRange = getAxisRange(Axis.X, xStart, xEnd);

                    // 2. 按Axis.Z规则生成Z轴遍历范围（整数）
                    int zStart = centerPos.getZ() - radius;
                    int zEnd = centerPos.getZ() + radius;
                    int[] zRange = getAxisRange(Axis.Z, zStart, zEnd);

                    // 遍历X/Z平面的圆环范围（适配Axis规则）
                    for (int dx : xRange) {
                        for (int dz : zRange) {
                            // 过滤圆环（曼哈顿距离，整数计算）
                            int dist = Math.abs(dx - centerPos.getX()) + Math.abs(dz - centerPos.getZ());
                            if (dist != radius) continue;

                            // 3. Y轴按Axis规则遍历（整数）
                            int yStart = Axis.Y.getStart(MyBox.this);
                            int yTarget = Axis.Y.getTarget(MyBox.this);
                            int yStep = Axis.Y.isIncrement(MyBox.this) ? 1 : -1;

                            for (int y = yStart; ; y += yStep) {
                                tempPos.set(dx, y, dz);

                                // 校验坐标是否在包围盒内（整数）
                                if (contains(tempPos)) {
                                    circleQueue.add(tempPos.immutable());
                                }

                                if (y == yTarget) break;
                            }
                        }
                    }
                }
            }

            /**
             * 按Axis规则生成轴的遍历范围（整数）
             */
            private int[] getAxisRange(Axis axis, int start, int end) {
                int length = Math.abs(end - start) + 1;
                int[] range = new int[length];

                if (axis.isIncrement(MyBox.this)) {
                    // 递增：从start到end
                    for (int i = 0; i < length; i++) {
                        range[i] = start + i;
                    }
                } else {
                    // 递减：从end到start
                    for (int i = 0; i < length; i++) {
                        range[i] = end - i;
                    }
                }

                return range;
            }

            @Override
            public boolean hasNext() {
                if (circleQueue.isEmpty()) {
                    resetCircleIterator();
                    return false;
                }
                return true;
            }

            @Override
            public BlockPos next() {
                return circleQueue.poll();
            }
        };
    }

    public enum Axis {
        X {
            @Override
            public int getMin(MyBox box) {
                return box.minX;
            }

            @Override
            public int getMax(MyBox box) {
                return box.maxX;
            }

            @Override
            public boolean isIncrement(MyBox box) {
                return box.xIncrement;
            }

            @Override
            public void setCoord(BlockPos.MutableBlockPos mutable, int value) {
                mutable.setX(value);
            }

            @Override
            public int getCoord(BlockPos pos) {
                return pos.getX();
            }
        },
        Y {
            @Override
            public int getMin(MyBox box) {
                return box.minY;
            }

            @Override
            public int getMax(MyBox box) {
                return box.maxY;
            }

            @Override
            public boolean isIncrement(MyBox box) {
                return box.yIncrement;
            }

            @Override
            public void setCoord(BlockPos.MutableBlockPos mutable, int value) {
                mutable.setY(value);
            }

            @Override
            public int getCoord(BlockPos pos) {
                return pos.getY();
            }
        },
        Z {
            @Override
            public int getMin(MyBox box) {
                return box.minZ;
            }

            @Override
            public int getMax(MyBox box) {
                return box.maxZ;
            }

            @Override
            public boolean isIncrement(MyBox box) {
                return box.zIncrement;
            }

            @Override
            public void setCoord(BlockPos.MutableBlockPos mutable, int value) {
                mutable.setZ(value);
            }

            @Override
            public int getCoord(BlockPos pos) {
                return pos.getZ();
            }
        };

        // 获取轴的最小值
        public abstract int getMin(MyBox box);

        // 获取轴的最大值
        public abstract int getMax(MyBox box);

        // 判断轴是否递增迭代
        public abstract boolean isIncrement(MyBox box);

        // 设置Mutable BlockPos的该轴坐标
        public abstract void setCoord(BlockPos.MutableBlockPos mutable, int value);

        // 获取BlockPos的该轴坐标
        public abstract int getCoord(BlockPos pos);

        // 获取轴的目标边界（递增取max，递减取min）
        public int getTarget(MyBox box) {
            return isIncrement(box) ? getMax(box) : getMin(box);
        }

        // 获取轴的起始边界（递增取min，递减取max）
        public int getStart(MyBox box) {
            return isIncrement(box) ? getMin(box) : getMax(box);
        }

        // 计算轴的下一个坐标值
        public int nextCoord(MyBox box, int current) {
            return current + (isIncrement(box) ? 1 : -1);
        }

        // 判断坐标是否超出轴的边界
        public boolean isOutOfBound(MyBox box, int current) {
            return isIncrement(box) ? current > getMax(box) : current < getMin(box);
        }
    }
}