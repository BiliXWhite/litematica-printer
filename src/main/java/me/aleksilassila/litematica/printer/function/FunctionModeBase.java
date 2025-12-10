package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.Printer;

import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public abstract class FunctionModeBase extends PrinterUtils implements FunctionExtension {
    public abstract State.PrintModeType getPrintModeType();

    public abstract ConfigBoolean getCurrentConfig();

    public boolean canTick() {
        if (InitHandler.MODE_SWITCH.getOptionListValue() instanceof State.ModeType modeType) {    // 当前模式
            // 如果是单模情况
            if (modeType.equals(State.ModeType.SINGLE)) {
                // 检查当前单模模式不等于本类的模式, 那么就不执行TICK
                if (InitHandler.PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType printModeType && !printModeType.equals(getPrintModeType())) {
                    return false;
                }
            }
            if (modeType.equals(State.ModeType.MULTI)){
                return getCurrentConfig().getBooleanValue();
            }
        }
        return true;
    }

    public abstract void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player);
}
