package me.aleksilassila.litematica.printer.function;

import me.aleksilassila.litematica.printer.printer.Printer;

public abstract class FunctionPlacement extends Function {
    @Override
    public boolean isConfigAllowExecute(Printer printer) {
        if (printer.placeSpeed != 0 && (printer.tickStartTime / 50) % printer.placeSpeed != 0) {
            return false;
        }
        return super.isConfigAllowExecute(printer);
    }
}
