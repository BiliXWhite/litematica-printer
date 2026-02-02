package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ModeType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;

public class ConfigUtils {
    public static boolean isEnable() {
        return Configs.General.WORK_TOGGLE.getBooleanValue();
    }

    public static boolean isPrintMode() {
        return (Configs.General.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Print.PRINT.getBooleanValue())
                || Configs.General.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.PRINTER;
    }

    public static boolean isMineMode() {
        return (Configs.General.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Excavate.MINE.getBooleanValue())
                || Configs.General.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.MINE;
    }

    public static boolean isFillMode() {
        return (Configs.General.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Fill.FILL.getBooleanValue())
                || Configs.General.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.FILL;
    }

    public static boolean isFluidMode() {
        return (Configs.General.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.FLUID.FLUID.getBooleanValue())
                || Configs.General.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.FLUID;
    }

    public static boolean isBedrockMode() {
        return (Configs.General.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Hotkeys.BEDROCK.getBooleanValue())
                || Configs.General.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.BEDROCK;
    }
}