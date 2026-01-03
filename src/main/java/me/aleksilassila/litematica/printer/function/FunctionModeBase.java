package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.ModeType;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.Printer;

import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public abstract class FunctionModeBase extends PrinterUtils implements FunctionExtension {
    public abstract PrintModeType getPrintModeType();

    public abstract ConfigBoolean getCurrentConfig();

    public boolean canTick() {
        if (Configs.MODE_SWITCH.getOptionListValue() instanceof ModeType modeType) {    // 当前模式
            // 如果是单模情况
            if (modeType.equals(ModeType.SINGLE)) {
                // 检查当前单模模式不等于本类的模式, 那么就不执行TICK
                if (Configs.PRINTER_MODE.getOptionListValue() instanceof PrintModeType printModeType && !printModeType.equals(getPrintModeType())) {
                    return false;
                }
            }
            if (modeType.equals(ModeType.MULTI)) {
                return getCurrentConfig().getBooleanValue();
            }
        }
        return true;
    }

    public abstract void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player);
}
