package me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite;

import fi.dy.masa.litematica.selection.Box;
import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MyBox extends AABB implements Iterable<BlockPos> {
    public boolean yIncrement = true;
    public boolean xIncrement = true;
    public boolean zIncrement = true;
    private Iterator<BlockPos> iterator;
    private IterationOrderType iterationMode = IterationOrderType.XZY;

    public void setIterationMode(IterationOrderType mode) {
        this.iterationMode = mode;
    }

    public MyBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
    }

    public MyBox(fi.dy.masa.litematica.selection.Box box) {
        this(Vec3.atLowerCornerOf(box.getPos1()), Vec3.atLowerCornerOf(box.getPos2()));
    }

    public MyBox(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public MyBox(Vec3 pos1, Vec3 pos2) {
        this(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
    }

    public MyBox(BlockPos pos1, BlockPos pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    //因原方法最大值比较时使用的是 < 而不是 <= 因此 最小边界能被覆盖 而最大边界不能
    @Override
    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(Vec3i vec3i) {
        return vec3i.getX() >= this.minX && vec3i.getX() <= this.maxX && vec3i.getY() >= this.minY && vec3i.getY() <= this.maxY && vec3i.getZ() >= this.minZ && vec3i.getZ() <= this.maxZ;
    }

    @Override
    public MyBox inflate(double x, double y, double z) {
        double d = this.minX - x;
        double e = this.minY - y;
        double f = this.minZ - z;
        double g = this.maxX + x;
        double h = this.maxY + y;
        double i = this.maxZ + z;
        return new MyBox(d, e, f, g, h, i);
    }

    @Override
    public MyBox inflate(double value) {
        return this.inflate(value, value, value);
    }

    public void initIterator() {


    }

    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        if (this.iterator == null) {
            this.iterator = new BoxIterator();
        }
        return this.iterator;
    }

    private class BoxIterator implements Iterator<BlockPos> {
        public BlockPos currPos;

        @Override
        public boolean hasNext() {
            if (currPos == null) return true;
            int x = currPos.getX();
            int y = currPos.getY();
            int z = currPos.getZ();

            int targetX = xIncrement ? (int) maxX : (int) minX;
            int targetY = yIncrement ? (int) maxY : (int) minY;
            int targetZ = zIncrement ? (int) maxZ : (int) minZ;

            return !(x == targetX && y == targetY && z == targetZ);
        }

        @Override
        public BlockPos next() {
            // 初始化起始位置
            if (currPos == null) {
                currPos = new BlockPos(
                        IterationOrderType.Axis.X.reset(MyBox.this),
                        IterationOrderType.Axis.Y.reset(MyBox.this),
                        IterationOrderType.Axis.Z.reset(MyBox.this)
                );
                return currPos;
            }

            // 复制当前坐标，避免直接修改原对象
            int x = currPos.getX();
            int y = currPos.getY();
            int z = currPos.getZ();

            // 获取当前迭代模式的轴优先级，通用处理所有轴迭代（无任何switch）
            for (IterationOrderType.Axis axis : iterationMode.axis) {
                // 对当前轴执行增量
                int newValue = axis.increment(MyBox.this, axis.getCoord(MyBox.this, x, y, z));

                // 检查是否溢出
                if (axis.isOverflow(MyBox.this, newValue)) {
                    // 溢出则重置当前轴，继续处理下一个轴
                    switch (axis) {
                        case X -> x = axis.reset(MyBox.this);
                        case Y -> y = axis.reset(MyBox.this);
                        case Z -> z = axis.reset(MyBox.this);
                    }
                } else {
                    // 未溢出则更新当前轴坐标，终止循环
                    switch (axis) {
                        case X -> x = newValue;
                        case Y -> y = newValue;
                        case Z -> z = newValue;
                    }
                    break;
                }
            }

            // 更新当前位置并返回
            currPos = new BlockPos(x, y, z);
            return currPos;
        }
    }
}