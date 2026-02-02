package me.aleksilassila.litematica.printer.function;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class FunctionBreak extends Function {
    @Override
    public boolean isConfigAllowExecute(Printer printer) {
        int breakSpeed = Configs.Break.BREAK_SPEED.getIntegerValue();
        if (breakSpeed != 0 && (printer.tickStartTime / 50) % breakSpeed != 0) {
            return false;
        }
        return super.isConfigAllowExecute(printer);
    }
}
