package me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite;

import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MyBox extends AABB implements Iterable<BlockPos> {
    public boolean yIncrement = true;
    public boolean xIncrement = true;
    public boolean zIncrement = true;
    private IterationOrderType iterationMode = IterationOrderType.XZY;
    private @Nullable BlockPos lastIteratedPos;

    public void setIterationMode(IterationOrderType mode) {
        this.iterationMode = mode;
    }

    public MyBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
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

    /**
     * 设置迭代器的起始位置（用于从上次终止的位置继续遍历）
     *
     * @param startPos 起始位置（传入null/结尾位置/无效位置则从头开始）
     */
    public void setStartPos(@Nullable BlockPos startPos) {
        if (startPos != null && contains(startPos) && !isEndPos(startPos)) {
            lastIteratedPos = startPos;
        }
    }

    private boolean isEndPos(BlockPos pos) {
        double targetX = xIncrement ? maxX : minX;
        double targetY = yIncrement ? maxY : minY;
        double targetZ = zIncrement ? maxZ : minZ;
        return pos.getX() == targetX && pos.getY() == targetY && pos.getZ() == targetZ;
    }


    //因原方法最大值比较时使用的是 < 而不是 <= 因此 最小边界能被覆盖 而最大边界不能
    @Override
    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public @NotNull MyBox inflate(double x, double y, double z) {
        double d = this.minX - x;
        double e = this.minY - y;
        double f = this.minZ - z;
        double g = this.maxX + x;
        double h = this.maxY + y;
        double i = this.maxZ + z;
        return new MyBox(d, e, f, g, h, i);
    }

    @Override
    public @NotNull MyBox inflate(double value) {
        return this.inflate(value, value, value);
    }


    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            public BlockPos currPos = lastIteratedPos;

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
                if (currPos == null) {
                    currPos = new BlockPos(
                            (int) (xIncrement ? minX : maxX),
                            (int) (yIncrement ? minY : maxY),
                            (int) (zIncrement ? minZ : maxZ)
                    );
                    return currPos;
                }

                int x = currPos.getX();
                int y = currPos.getY();
                int z = currPos.getZ();

                if (iterationMode.equals(IterationOrderType.XZY)) {
                    x += xIncrement ? 1 : -1;
                    if (xIncrement ? x > maxX : x < minX) {
                        x = (int) (xIncrement ? minX : maxX);
                        z += zIncrement ? 1 : -1;
                        if (zIncrement ? z > maxZ : z < minZ) {
                            z = (int) (zIncrement ? minZ : maxZ);
                            y += yIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(IterationOrderType.XYZ)) {
                    x += xIncrement ? 1 : -1;
                    if (xIncrement ? x > maxX : x < minX) {
                        x = (int) (xIncrement ? minX : maxX);
                        y += yIncrement ? 1 : -1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = (int) (yIncrement ? minY : maxY);
                            z += zIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(IterationOrderType.YXZ)) {
                    y += yIncrement ? 1 : -1;
                    if (yIncrement ? y > maxY : y < minY) {
                        y = (int) (yIncrement ? minY : maxY);
                        x += xIncrement ? 1 : -1;
                        if (xIncrement ? x > maxX : x < minX) {
                            x = (int) (xIncrement ? minX : maxX);
                            z += zIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(IterationOrderType.YZX)) {
                    y += yIncrement ? 1 : -1;
                    if (yIncrement ? y > maxY : y < minY) {
                        y = (int) (yIncrement ? minY : maxY);
                        z += zIncrement ? 1 : -1;
                        if (zIncrement ? z > maxZ : z < minZ) {
                            z = (int) (zIncrement ? minZ : maxZ);
                            x += xIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(IterationOrderType.ZXY)) {
                    z += zIncrement ? 1 : -1;
                    if (zIncrement ? z > maxZ : z < minZ) {
                        z = (int) (zIncrement ? minZ : maxZ);
                        x += xIncrement ? 1 : -1;
                        if (xIncrement ? x > maxX : x < minX) {
                            x = (int) (xIncrement ? minX : maxX);
                            y += yIncrement ? 1 : -1;
                        }
                    }
                } else if (iterationMode.equals(IterationOrderType.ZYX)) {
                    z += zIncrement ? 1 : -1;
                    if (zIncrement ? z > maxZ : z < minZ) {
                        z = (int) (zIncrement ? minZ : maxZ);
                        y += yIncrement ? 1 : -1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = (int) (yIncrement ? minY : maxY);
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