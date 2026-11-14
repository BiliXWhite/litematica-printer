package me.aleksilassila.litematica.printer.compat;

import me.aleksilassila.litematica.printer.Reference;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class LitematicaCompat {
    private static boolean resolved = false;

    @Nullable
    private static Method setEasyPlaceLastPickBlockTimeMethod;

    private LitematicaCompat() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        // 0.19.61+
        setEasyPlaceLastPickBlockTimeMethod = findMethod("fi.dy.masa.litematica.util.EasyPlaceUtils");
        // 更早的
        if (setEasyPlaceLastPickBlockTimeMethod == null) {
            setEasyPlaceLastPickBlockTimeMethod = findMethod("fi.dy.masa.litematica.util.WorldUtils");
        }
    }

    @Nullable
    private static Method findMethod(String className) {
        try {
            Method method = Class.forName(className).getDeclaredMethod("setEasyPlaceLastPickBlockTime");
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // 重置投影的“选取方块”时间，防止被限速换回去
    public static void setEasyPlaceLastPickBlockTime() {
        if (!resolved) {
            resolve();
        }
        if (setEasyPlaceLastPickBlockTimeMethod == null) {
            return;
        }
        try {
            setEasyPlaceLastPickBlockTimeMethod.invoke(null);
        } catch (Throwable t) {
            Reference.LOGGER.warn("Failed to call Litematica#setEasyPlaceLastPickBlockTime", t);
        }
    }
}
