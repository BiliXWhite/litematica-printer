package me.aleksilassila.litematica.printer.iterator;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.IterationModeType;
import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class IteratorOptions {
    private static final Supplier<BlockPos> playerPosSupplier = () -> {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getOnPos();
        }
        return BlockPos.ZERO;
    };
    private static final Supplier<IterationOrderType> orderSupplier = () -> (IterationOrderType) Configs.General.ITERATION_ORDER.getOptionListValue();
    private static final Supplier<IterationModeType> modeSupplier = () -> (IterationModeType) Configs.General.ITERATION_MODE.getOptionListValue();
    private static final BooleanSupplier xIncrementSupplier = () -> !Configs.General.X_REVERSE.getBooleanValue();
    private static final BooleanSupplier yIncrementSupplier = () -> !Configs.General.Y_REVERSE.getBooleanValue();
    private static final BooleanSupplier zIncrementSupplier = () -> !Configs.General.Z_REVERSE.getBooleanValue();
    private static final BooleanSupplier circleDirectionSupplier = Configs.General.CIRCLE_DIRECTION::getBooleanValue;

    public IterationOrderType order;
    public IterationModeType mode;
    public boolean xIncrement;
    public boolean yIncrement;
    public boolean zIncrement;
    public boolean circleDirection;

    public IteratorOptions() {
        update();
    }

    public boolean update() {
        IterationOrderType order = orderSupplier.get();
        IterationModeType mode = modeSupplier.get();
        boolean xIncrement = xIncrementSupplier.getAsBoolean();
        boolean yIncrement = yIncrementSupplier.getAsBoolean();
        boolean zIncrement = zIncrementSupplier.getAsBoolean();
        boolean circleDirection = circleDirectionSupplier.getAsBoolean();
        boolean changed= order != this.order
                || mode != this.mode
                || xIncrement != this.xIncrement
                || yIncrement != this.yIncrement
                || zIncrement != this.zIncrement
                || circleDirection != this.circleDirection;
        if (changed){
            this.order = order;
            this.mode = mode;
            this.xIncrement = xIncrement;
            this.yIncrement = yIncrement;
            this.zIncrement = zIncrement;
            this.circleDirection = circleDirection;
        }
        return changed;
    }

    public Snapshot toSnapshot() {
        return new Snapshot(order, mode, xIncrement, yIncrement, zIncrement, circleDirection);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IteratorOptions that = (IteratorOptions) o;
        return yIncrement == that.yIncrement && xIncrement == that.xIncrement && zIncrement == that.zIncrement && circleDirection == that.circleDirection && order == that.order && mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, mode, yIncrement, xIncrement, zIncrement, circleDirection);
    }

    public record Snapshot(IterationOrderType order, IterationModeType mode, boolean xIncrement, boolean yIncrement,
                           boolean zIncrement, boolean circleDirection) {
    }
}
