package me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite;

import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import net.minecraft.core.BlockPos;

import static me.aleksilassila.litematica.printer.config.enums.IterationOrderType.*;

public class MyBox implements Iterable<BlockPos> {
    public boolean yIncrement = true;
    public boolean xIncrement = true;
    public boolean zIncrement = true;
    public Iterator<BlockPos> iterator;
    private IterationOrderType iterationMode = XZY;

    public void setIterationMode(IterationOrderType mode) {
        this.initIterator();
        this.iterationMode = mode;
    }

    public final int minX;
    public final int minY;
    public final int minZ;
    public final int maxX;
    public final int maxY;
    public final int maxZ;

    public MyBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }


    public MyBox(BlockPos pos1, BlockPos pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public MyBox(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX + 1
                && y >= this.minY && y <= this.maxY + 1
                && z >= this.minZ && z <= this.maxZ + 1;
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public MyBox resize(int x, int y, int z) {
        int minX = this.minX - x;
        int minY = this.minY - y;
        int minZ = this.minZ - z;
        int maxX = this.maxX + x;
        int maxY = this.maxY + y;
        int maxZ = this.maxZ + z;
        return new MyBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public MyBox resize(int value) {
        return this.resize(value, value, value);
    }

    private void initIterator() {
        if (this.iterator == null) this.iterator = iterator();

    }

    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            public BlockPos currPos;

            @Override
            public boolean hasNext() {
                if (currPos == null) return true;
                int x = currPos.getX();
                int y = currPos.getY();
                int z = currPos.getZ();

                int targetX = xIncrement ? maxX : minX;
                int targetY = yIncrement ? maxY : minY;
                int targetZ = zIncrement ? maxZ : minZ;

                return !(x == targetX && y == targetY && z == targetZ);
            }

            @Override
            public BlockPos next() {
                if (currPos == null) {
                    currPos = new BlockPos(
                            xIncrement ? minX : maxX,
                            yIncrement ? minY : maxY,
                            zIncrement ? minZ : maxZ
                    );
                    return currPos;
                }

                int x = currPos.getX();
                int y = currPos.getY();
                int z = currPos.getZ();

                if (iterationMode.equals(XZY)) {
                    x += xIncrement ? 1 : -1;
                    if (xIncrement ? x > maxX : x < minX) {
                        x = (xIncrement ? minX : maxX);
                        z += zIncrement ? 1 : -1;
                        if (zIncrement ? z > maxZ : z < minZ) {
                            z = (zIncrement ? minZ : maxZ);
                            y += yIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(XYZ)) {
                    x += xIncrement ? 1 : -1;
                    if (xIncrement ? x > maxX : x < minX) {
                        x = (xIncrement ? minX : maxX);
                        y += yIncrement ? 1 : -1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = (yIncrement ? minY : maxY);
                            z += zIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(YXZ)) {
                    y += yIncrement ? 1 : -1;
                    if (yIncrement ? y > maxY : y < minY) {
                        y = (yIncrement ? minY : maxY);
                        x += xIncrement ? 1 : -1;
                        if (xIncrement ? x > maxX : x < minX) {
                            x = (xIncrement ? minX : maxX);
                            z += zIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(YZX)) {
                    y += yIncrement ? 1 : -1;
                    if (yIncrement ? y > maxY : y < minY) {
                        y = (yIncrement ? minY : maxY);
                        z += zIncrement ? 1 : -1;
                        if (zIncrement ? z > maxZ : z < minZ) {
                            z = (zIncrement ? minZ : maxZ);
                            x += xIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(ZXY)) {
                    z += zIncrement ? 1 : -1;
                    if (zIncrement ? z > maxZ : z < minZ) {
                        z = (zIncrement ? minZ : maxZ);
                        x += xIncrement ? 1 : -1;
                        if (xIncrement ? x > maxX : x < minX) {
                            x = (xIncrement ? minX : maxX);
                            y += yIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(ZYX)) {
                    z += zIncrement ? 1 : -1;
                    if (zIncrement ? z > maxZ : z < minZ) {
                        z = (zIncrement ? minZ : maxZ);
                        y += yIncrement ? 1 : -1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = (yIncrement ? minY : maxY);
                            x += xIncrement ? 1 : -1;
                        }
                    }
                } else {
                    throw new IllegalStateException("Unexpected value: " + iterationMode);
                }

                currPos = new BlockPos(x, y, z);
                return currPos;
            }
        };
    }
}
