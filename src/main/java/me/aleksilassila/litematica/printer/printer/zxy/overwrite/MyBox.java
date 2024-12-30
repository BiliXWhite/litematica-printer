package me.aleksilassila.litematica.printer.printer.zxy.overwrite;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class MyBox extends Box implements Iterable<BlockPos> {
    public boolean yIncrement = true;
    public Iterator<BlockPos> iterator;
    public MyBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
    }

    public MyBox(fi.dy.masa.litematica.selection.Box box) {
        this(Vec3d.of(box.getPos1()), Vec3d.of(box.getPos2()));
    }

    public MyBox(BlockPos pos) {
        this((double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), (double) (pos.getX()), (double) (pos.getY()), (double) (pos.getZ()));
    }

    public MyBox(Vec3d pos1, Vec3d pos2) {
        this(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
    }

    //因原方法最大值比较时使用的是 < 而不是 <= 因此 最小边界能被覆盖 而最大边界不能
    @Override
    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }
    @Override
    public MyBox expand(double x, double y, double z) {
        double d = this.minX - x;
        double e = this.minY - y;
        double f = this.minZ - z;
        double g = this.maxX + x;
        double h = this.maxY + y;
        double i = this.maxZ + z;
        return new MyBox(d, e, f, g, h, i);
    }
    @Override
    public MyBox expand(double value) {
        return this.expand(value, value, value);
    }
    public void initIterator(){
        if (this.iterator == null) this.iterator = iterator();
    }
    public void resetIterator(){
        this.iterator = iterator();
    }
    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        return new Iterator<BlockPos>() {
            public BlockPos currPos;
            @Override
            public boolean hasNext() {
                if (currPos == null) return true;
                int x = currPos.getX();
                int y = currPos.getY();
                int z = currPos.getZ();
                boolean b = !(x == maxX && (yIncrement ? y == maxY : y == minY) && z == maxZ);
                if (!b) currPos = null;
                return b;
            }

            @Override
            public BlockPos next() {
                if (currPos == null) {
                    currPos = new BlockPos((int) minX, (int) (yIncrement ? minY : maxY), (int) minZ);
                    return currPos;
                }
                int x = currPos.getX();
                int y = currPos.getY();
                int z = currPos.getZ();
                x++;
                if (x > maxX) {
                    x = (int) minX;
                    z++;
                    if (z > maxZ) {
                        z = (int) minZ;
                        y = yIncrement ? y + 1 : y - 1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = (int) (yIncrement ? minY : maxY);
                        }
                    }
                }
                currPos = new BlockPos(x, y, z);
                return currPos;
            }
        };
    }
}
