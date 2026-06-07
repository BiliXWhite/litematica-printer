package me.aleksilassila.litematica.printer.printer;

import lombok.Getter;
import net.minecraft.core.BlockPos;

@Getter
public class QueuedOperation {
    private final BlockPos pos;
    private final boolean isRepair;
    private int age;

    public QueuedOperation(BlockPos pos, boolean isRepair) {
        this.pos = pos;
        this.isRepair = isRepair;
        this.age = 0;
    }

    public void incrementAge() {
        this.age++;
    }
}
