package me.aleksilassila.litematica.printer.function;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;

public abstract class FunctionBreak extends Function {
    @Override
    public boolean isConfigAllowExecute(Printer printer) {
        int breakInterval = Configs.Break.BREAK_INTERVAL.getIntegerValue();
        if (breakInterval != 0 && (printer.tickStartTime / 50) % breakInterval != 0) {
            return false;
        }
        return super.isConfigAllowExecute(printer);
    }
}
