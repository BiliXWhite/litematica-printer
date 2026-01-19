package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;
import me.aleksilassila.litematica.printer.printer.MyBox;

public enum IterationOrderType implements ConfigOptionListEntry<IterationOrderType> {
    XYZ(I18n.config("iterationOrder.xyz"), Axis.X, Axis.Y, Axis.Z),
    XZY(I18n.config("iterationOrder.xzy"), Axis.X, Axis.Z, Axis.Y),
    YXZ(I18n.config("iterationOrder.yxz"), Axis.Y, Axis.X, Axis.Z),
    YZX(I18n.config("iterationOrder.yzx"), Axis.Y, Axis.Z, Axis.X),
    ZXY(I18n.config("iterationOrder.zxy"), Axis.Z, Axis.X, Axis.Y),
    ZYX(I18n.config("iterationOrder.zyx"), Axis.Z, Axis.Y, Axis.X);

    private final I18n i18n;
    public final Axis[] axis;

    IterationOrderType(I18n i18n, Axis... axis) {
        this.i18n = i18n;
        this.axis = axis;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }

    // 封装轴的所有行为逻辑
    public enum Axis {
        X {
            @Override
            public int getCoord(MyBox box, int x, int y, int z) {
                return x;
            }

            @Override
            public int increment(MyBox box, int current) {
                return current + (box.xIncrement ? 1 : -1);
            }

            @Override
            public boolean isOverflow(MyBox box, int value) {
                return box.xIncrement ? value > box.maxX : value < box.minX;
            }

            @Override
            public int reset(MyBox box) {
                return (int) (box.xIncrement ? box.minX : box.maxX);
            }
        },
        Y {
            @Override
            public int getCoord(MyBox box, int x, int y, int z) {
                return y;
            }

            @Override
            public int increment(MyBox box, int current) {
                return current + (box.yIncrement ? 1 : -1);
            }

            @Override
            public boolean isOverflow(MyBox box, int value) {
                return box.yIncrement ? value > box.maxY : value < box.minY;
            }

            @Override
            public int reset(MyBox box) {
                return (int) (box.yIncrement ? box.minY : box.maxY);
            }
        },
        Z {
            @Override
            public int getCoord(MyBox box, int x, int y, int z) {
                return z;
            }

            @Override
            public int increment(MyBox box, int current) {
                return current + (box.zIncrement ? 1 : -1);
            }

            @Override
            public boolean isOverflow(MyBox box, int value) {
                return box.zIncrement ? value > box.maxZ : value < box.minZ;
            }

            @Override
            public int reset(MyBox box) {
                return (int) (box.zIncrement ? box.minZ : box.maxZ);
            }
        };

        // 获取当前轴的坐标值（x/y/z）
        public abstract int getCoord(MyBox box, int x, int y, int z);

        // 对当前轴执行增量操作
        public abstract int increment(MyBox box, int current);

        // 检查当前轴是否超出边界
        public abstract boolean isOverflow(MyBox box, int value);

        // 重置当前轴到起始边界值
        public abstract int reset(MyBox box);
    }
}