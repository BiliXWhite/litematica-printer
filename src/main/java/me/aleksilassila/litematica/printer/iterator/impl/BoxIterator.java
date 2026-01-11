package me.aleksilassila.litematica.printer.iterator.impl;

import me.aleksilassila.litematica.printer.iterator.IteratorContext;
import me.aleksilassila.litematica.printer.iterator.IteratorOptions;
import net.minecraft.core.BlockPos;

import java.util.Iterator;

public abstract class BoxIterator implements Iterator<BlockPos> {
    public final IteratorContext context = new IteratorContext();

    public abstract void reset();

    public boolean update() {
        if (this.context.update()) {
            reset(); // 重置迭代器, 重新创建一个
            return true;
        }
        return false;
    }
}
