package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.enums.IterationOrderType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

import net.minecraft.core.BlockPos;

public class PrinterBox implements Iterable<BlockPos> {
    public static final Minecraft client = Minecraft.getInstance();
    public final int minX, minY, minZ;
    public final int maxX, maxY, maxZ;
    public boolean yIncrement = true;
    public boolean xIncrement = true;
    public boolean zIncrement = true;
    public IterationOrderType iterationMode = IterationOrderType.XZY;
    private Iterator<BlockPos> iterator;

    public PrinterBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        int rawMinY = Math.min(minY, maxY);
        int rawMaxY = Math.max(minY, maxY);
        if (client.level != null) {
            this.minY = Math.max(client.level.getMinY(), rawMinY);
            this.maxY = Math.min(client.level.getMaxY(), rawMaxY);
        } else {
            this.minY = rawMinY;
            this.maxY = rawMaxY;
        }
    }

    public PrinterBox(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public PrinterBox(Vec3i pos1, Vec3i pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(Vec3i vec3i) {
        return vec3i.getX() >= this.minX && vec3i.getX() <= this.maxX && vec3i.getY() >= this.minY && vec3i.getY() <= this.maxY && vec3i.getZ() >= this.minZ && vec3i.getZ() <= this.maxZ;
    }

    public PrinterBox expand(int expandX, int expandY, int expandZ) {
        int minX = this.minX - expandX;
        int minZ = this.minZ - expandZ;
        int maxX = this.maxX + expandX;
        int maxZ = this.maxZ + expandZ;
        int minY = this.minY - expandY;
        int maxY = this.maxY + expandY;
        if (client.level != null) {
            minY = Math.max(client.level.getMinY(), minY);
            maxY = Math.min(client.level.getMaxY(), maxY);
        }
        return new PrinterBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public PrinterBox expand(int value) {
        return this.expand(value, value, value);
    }


    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        if (this.iterator == null) {
            this.iterator = new BoxIterator();
        }
        return this.iterator;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PrinterBox box = (PrinterBox) o;
        return minX == box.minX && minY == box.minY && minZ == box.minZ && maxX == box.maxX && maxY == box.maxY && maxZ == box.maxZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private class BoxIterator implements Iterator<BlockPos> {
        private int x, y, z;
        private boolean initialized = false;

        @Override
        public boolean hasNext() {
            if (!initialized) return true;
            int tx = xIncrement ? maxX : minX;
            int ty = yIncrement ? maxY : minY;
            int tz = zIncrement ? maxZ : minZ;
            return !(x == tx && y == ty && z == tz);
        }

        @Override
        public BlockPos next() {
            if (!initialized) {
                x = xIncrement ? minX : maxX;
                y = yIncrement ? minY : maxY;
                z = zIncrement ? minZ : maxZ;
                initialized = true;
                return new BlockPos(x, y, z);
            }

            // 根据迭代模式内联展开，消除多态派发
            switch (iterationMode) {
                case XYZ:
                    x += xIncrement ? 1 : -1;
                    if (xIncrement ? x > maxX : x < minX) {
                        x = xIncrement ? minX : maxX;
                        y += yIncrement ? 1 : -1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = yIncrement ? minY : maxY;
                            z += zIncrement ? 1 : -1;
                        }
                    }
                    break;
                case XZY:
                    x += xIncrement ? 1 : -1;
                    if (xIncrement ? x > maxX : x < minX) {
                        x = xIncrement ? minX : maxX;
                        z += zIncrement ? 1 : -1;
                        if (zIncrement ? z > maxZ : z < minZ) {
                            z = zIncrement ? minZ : maxZ;
                            y += yIncrement ? 1 : -1;
                        }
                    }
                    break;
                case YXZ:
                    y += yIncrement ? 1 : -1;
                    if (yIncrement ? y > maxY : y < minY) {
                        y = yIncrement ? minY : maxY;
                        x += xIncrement ? 1 : -1;
                        if (xIncrement ? x > maxX : x < minX) {
                            x = xIncrement ? minX : maxX;
                            z += zIncrement ? 1 : -1;
                        }
                    }
                    break;
                case YZX:
                    y += yIncrement ? 1 : -1;
                    if (yIncrement ? y > maxY : y < minY) {
                        y = yIncrement ? minY : maxY;
                        z += zIncrement ? 1 : -1;
                        if (zIncrement ? z > maxZ : z < minZ) {
                            z = zIncrement ? minZ : maxZ;
                            x += xIncrement ? 1 : -1;
                        }
                    }
                    break;
                case ZXY:
                    z += zIncrement ? 1 : -1;
                    if (zIncrement ? z > maxZ : z < minZ) {
                        z = zIncrement ? minZ : maxZ;
                        x += xIncrement ? 1 : -1;
                        if (xIncrement ? x > maxX : x < minX) {
                            x = xIncrement ? minX : maxX;
                            y += yIncrement ? 1 : -1;
                        }
                    }
                    break;
                case ZYX:
                    z += zIncrement ? 1 : -1;
                    if (zIncrement ? z > maxZ : z < minZ) {
                        z = zIncrement ? minZ : maxZ;
                        y += yIncrement ? 1 : -1;
                        if (yIncrement ? y > maxY : y < minY) {
                            y = yIncrement ? minY : maxY;
                            x += xIncrement ? 1 : -1;
                        }
                    }
                    break;
            }

            return new BlockPos(x, y, z);
        }
    }
}