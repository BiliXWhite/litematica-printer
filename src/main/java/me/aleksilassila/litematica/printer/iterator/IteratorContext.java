package me.aleksilassila.litematica.printer.iterator;

import me.aleksilassila.litematica.printer.config.enums.IterationModeType;
import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import me.aleksilassila.litematica.printer.printer.PrinterBox;

import java.util.Objects;

public class IteratorContext {
    public final PrinterBox box;
    public final IteratorOptions options;

    public IteratorContext() {
        this.box = new PrinterBox();
        this.options = new IteratorOptions();
    }

    public boolean update() {
        boolean result = this.box.update();
        if (this.options.update()){
            result = true;
        }
        return result;
    }

    public int minX() {
        return this.box.minX();
    }

    public int minY() {
        return this.box.minY();
    }

    public int minZ() {
        return this.box.minZ();
    }

    public int maxX() {
        return this.box.maxX();
    }

    public int maxY() {
        return this.box.maxY();
    }

    public int maxZ() {
        return this.box.maxZ();
    }

    public IterationOrderType order() {
        return this.options.order;
    }

    public IterationModeType mode() {
        return this.options.mode;
    }

    public boolean yIncrement() {
        return this.options.yIncrement;
    }

    public boolean xIncrement() {
        return this.options.xIncrement;
    }

    public boolean zIncrement() {
        return this.options.zIncrement;
    }

    public boolean circleDirection() {
        return this.options.circleDirection;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IteratorContext that = (IteratorContext) o;
        return Objects.equals(box, that.box) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(box, options);
    }
}
