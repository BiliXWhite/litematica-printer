package me.aleksilassila.litematica.printer;

import static me.aleksilassila.litematica.printer.InitHandler.DEBUG_OUTPUT;

/**
 * 调试日志输出类
 */
public class Debug {
    public static void alwaysWrite(String var1, Object... var2) {
        LitematicaPrinterMod.LOGGER.info(var1, var2);
    }

    public static void alwaysWrite(Object obj) {
        LitematicaPrinterMod.LOGGER.info(obj.toString());
    }

    public static void alwaysWrite() {
        alwaysWrite("");
    }

    public static void write(String var1, Object... var2) {
        if (DEBUG_OUTPUT.getBooleanValue()) {
            LitematicaPrinterMod.LOGGER.info(var1, var2);
        }
    }

    public static void write(Object obj) {
        if (DEBUG_OUTPUT.getBooleanValue()) {
            LitematicaPrinterMod.LOGGER.info(obj.toString());
        }
    }

    public static void write() {
        write("");
    }
}
