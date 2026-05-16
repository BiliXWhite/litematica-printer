package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public class ConfigUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();

    public static boolean isPrinterEnable() {
        return Configs.Core.WORK_SWITCH.getBooleanValue();
    }

    public static boolean isMultiMode() {
        return Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI);
    }

    public static boolean isSingleMode() {
        return Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.SINGLE);
    }

    public static boolean isPrintMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI) && Configs.Core.PRINT.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.PRINTER;
    }

    public static boolean isMineMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI) && Configs.Core.MINE.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.MINE;
    }

    public static boolean isFillMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI) && Configs.Core.FILL.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.FILL;
    }

    public static boolean isFluidMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI) && Configs.Core.FLUID.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.FLUID;
    }

    public static boolean isBedrockMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI) && Configs.Hotkeys.BEDROCK.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.BEDROCK;
    }

    public static PrintModeType getPrintModeType() {
        return (PrintModeType) Configs.Core.WORK_MODE_TYPE.getOptionListValue();
    }

    public static int getPlaceCooldown() {
        return Configs.Placement.PLACE_COOLDOWN.getIntegerValue();
    }

    public static int getBreakCooldown() {
        return Configs.Break.BREAK_COOLDOWN.getIntegerValue();
    }

    public static int getWorkRange() {
        return Configs.Core.WORK_RANGE.getIntegerValue();
    }

    public static double getEffectiveRange() {
        int configRange = Configs.Core.WORK_RANGE.getIntegerValue();
        if (configRange <= 0) {
            return PlayerUtils.getInteractionRange(6);
        }
        return configRange;
    }

    public static Direction getFillModeFacing() {
        if (Configs.Fill.FILL_BLOCK_FACING.getOptionListValue() instanceof FillModeFacingType fillModeFacingType) {
            return switch (fillModeFacingType) {
                case DOWN -> Direction.DOWN;
                case UP -> Direction.UP;
                case WEST -> Direction.WEST;
                case EAST -> Direction.EAST;
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
                default -> null;
            };
        }
        return null;
    }
}