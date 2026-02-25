package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class ConfigUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();

    public static boolean isEnable() {
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

    public static boolean canInteracted(BlockPos blockPos) {
        double workRange = getWorkRange();
        if (Configs.Core.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()) {
            if (client.player != null && !PlayerUtils.isWithinBlockInteractionRange(client.player, blockPos, 1F)) {
                return false;
            }
        }
        if (Configs.Core.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType radiusShapeType) {
            return switch (radiusShapeType) {
                case SPHERE -> PlayerUtils.isWithinWorkInteractedEuclideanRange(blockPos, workRange);
                case OCTAHEDRON -> PlayerUtils.isWithinWorkInteractedManhattanRange(blockPos, workRange);
                case CUBE -> PlayerUtils.isWithinWorkInteractedCubeRange(blockPos, workRange);
            };
        }
        return true;
    }

    public static boolean isPositionInSelectionRange(Player player, @NotNull BlockPos pos, ConfigOptionList selectionTypeConfig) {
        if (player == null || selectionTypeConfig == null) {
            return false;
        }
        if (!(selectionTypeConfig.getOptionListValue() instanceof SelectionType selectionType)) {
            return false;
        }
        return switch (selectionType) {
            case LITEMATICA_RENDER_LAYER -> LitematicaUtils.isPositionWithinRange(pos);
            case LITEMATICA_SELECTION_BELOW_PLAYER -> pos.getY() <= Math.floor(player.getY());
            case LITEMATICA_SELECTION_ABOVE_PLAYER -> pos.getY() >= Math.ceil(player.getY());
            default -> true;
        };
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