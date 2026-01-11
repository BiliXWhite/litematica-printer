package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrinterBox {
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    public PrinterBox() {
        update();
    }

    public PrinterBox(BlockPos pos1, BlockPos pos2) {
        update(pos1, pos2);
    }

    public PrinterBox(BlockPos pos) {
        update(pos);
    }

    public void update(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int dx, int dy, int dz) {
        this.minX = minX - dx;
        this.minY = minY - dy;
        this.minZ = minZ - dz;
        this.maxX = maxX + dx;
        this.maxY = maxY + dy;
        this.maxZ = maxZ + dz;
        if (this.maxX < this.minX || this.maxY < this.minY || this.maxZ < this.minZ) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }
    }

    public void update(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int radius) {
        update(minX, minY, minZ, maxX, maxY, maxZ, radius, radius, radius);
    }

    public void update(BlockPos pos1, BlockPos pos2, int dx, int dy, int dz) {
        update(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), dx, dy, dz);
    }

    public void update(BlockPos pos1, BlockPos pos2, int radius) {
        update(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), radius);
    }

    public void update(BlockPos pos1, BlockPos pos) {
        update(pos1, pos, 0);
    }

    public void update(BlockPos pos, int dx, int dy, int dz) {
        update(pos, pos, dx, dy, dz);
    }

    public void update(BlockPos pos, int radius) {
        update(pos, pos, radius);
    }

    public void update(BlockPos pos) {
        update(pos, 0);
    }

    public boolean update() {
        AtomicBoolean result = new AtomicBoolean(false);
        PlayerUtils.getPlayer().ifPresent(player -> {
                    BlockPos playerOnPos = player.getOnPos();
                    int workRangeInt = PrinterUtils.getWorkRangeInt();
                    int minX = playerOnPos.getX() - workRangeInt;
                    int minY = playerOnPos.getY() - workRangeInt;
                    int minZ = playerOnPos.getZ() - workRangeInt;
                    int maxX = playerOnPos.getX() + workRangeInt;
                    int maxY = playerOnPos.getY() + workRangeInt;
                    int maxZ = playerOnPos.getZ() + workRangeInt;
                    if (minX != this.minX || minY != this.minY || minZ != this.minZ || maxX != this.maxX || maxY != this.maxY || maxZ != this.maxZ) {
                        update(player.getOnPos(), PrinterUtils.getWorkRangeInt());
                        result.set(true);
                    }
                }
        );
        return result.get();
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX
                && y >= this.minY && y <= this.maxY
                && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(Vec3i pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
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

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }
}