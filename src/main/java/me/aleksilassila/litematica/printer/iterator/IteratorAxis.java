package me.aleksilassila.litematica.printer.iterator;

import net.minecraft.core.BlockPos;

public enum IteratorAxis {
    X {
        @Override
        public int getMin(IteratorContext context) {
            return context.minX();
        }

        @Override
        public int getMax(IteratorContext context) {
            return context.maxX();
        }

        @Override
        public boolean isIncrement(IteratorContext context) {
            return context.xIncrement();
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
        public int getMin(IteratorContext context) {
            return context.minY();
        }

        @Override
        public int getMax(IteratorContext context) {
            return context.maxY();
        }

        @Override
        public boolean isIncrement(IteratorContext context) {
            return context.yIncrement();
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
        public int getMin(IteratorContext context) {
            return context.minZ();
        }

        @Override
        public int getMax(IteratorContext context) {
            return context.maxZ();
        }

        @Override
        public boolean isIncrement(IteratorContext context) {
            return context.zIncrement();
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
    public abstract int getMin(IteratorContext context);

    // 获取轴的最大值
    public abstract int getMax(IteratorContext context);

    // 判断轴是否递增迭代
    public abstract boolean isIncrement(IteratorContext context);

    // 设置Mutable BlockPos的该轴坐标
    public abstract void setCoord(BlockPos.MutableBlockPos mutable, int value);

    // 获取BlockPos的该轴坐标
    public abstract int getCoord(BlockPos pos);

    // 获取轴的目标边界（递增取max，递减取min）
    public int getTarget(IteratorContext context) {
        return isIncrement(context) ? getMax(context) : getMin(context);
    }

    // 获取轴的起始边界（递增取min，递减取max）
    public int getStart(IteratorContext context) {
        return isIncrement(context) ? getMin(context) : getMax(context);
    }

    // 计算轴的下一个坐标值
    public int nextCoord(IteratorContext context, int current) {
        return current + (isIncrement(context) ? 1 : -1);
    }

    // 判断坐标是否超出轴的边界
    public boolean isOutOfBound(IteratorContext context, int current) {
        return isIncrement(context) ? current > getMax(context) : current < getMin(context);
    }
}
