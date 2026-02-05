package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ModeType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.enums.RadiusShapeType;
import me.aleksilassila.litematica.printer.enums.SelectionType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConfigUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();

    public static boolean isEnable() {
        return Configs.Core.WORK_SWITCH.getBooleanValue();
    }

    public static boolean isPrintMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Core.PRINT.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.PRINTER;
    }

    public static boolean isMineMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Core.MINE.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.MINE;
    }

    public static boolean isFillMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Core.FILL.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.FILL;
    }

    public static boolean isFluidMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Core.FLUID.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.FLUID;
    }

    public static boolean isBedrockMode() {
        return (Configs.Core.WORK_MODE.getOptionListValue().equals(ModeType.MULTI) && Configs.Hotkeys.BEDROCK.getBooleanValue())
                || Configs.Core.WORK_MODE_TYPE.getOptionListValue() == PrintModeType.BEDROCK;
    }

    public static int getWorkRange() {
        return Configs.Core.WORK_RANGE.getIntegerValue();
    }

    public static int getPlaceCooldown() {
        return Configs.Placement.PLACE_COOLDOWN.getIntegerValue();
    }

    public static int getBreakCooldown() {
        return Configs.Break.BREAK_COOLDOWN.getIntegerValue();
    }

    public static boolean canInteracted(BlockPos blockPos) {
        double workRange = getWorkRange();
        if (Configs.Core.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()) {
            if (client.player != null && !PlayerUtils.canInteractWithBlockAt(client.player, blockPos, 1F)) {
                return false;
            }
        }
        if (Configs.Core.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType radiusShapeType) {
            return switch (radiusShapeType) {
                case SPHERE -> PlayerUtils.canInteractedEuclidean(blockPos, workRange);
                case OCTAHEDRON -> PlayerUtils.canInteractedManhattan(blockPos, workRange);
                case CUBE -> PlayerUtils.canInteractedCube(blockPos, workRange);
            };
        }
        return true;
    }

    /**
     * 判断位置是否位于当前加载的投影范围内。
     *
     * @param pos 要检测的方块位置
     * @return 如果位置属于图纸结构的一部分，则返回 true，否则返回 false
     */
    public static boolean isSchematicBlock(BlockPos pos) {
        SchematicPlacementManager schematicPlacementManager = DataManager.getSchematicPlacementManager();
        //#if MC < 11900
        //$$ List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingSubChunk(new SubChunkPos(pos));
        //#else
        List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingChunk(pos);
        //#endif

        for (SchematicPlacementManager.PlacementPart placementPart : allPlacementsTouchingChunk) {
            if (placementPart.getBox().containsPos(pos)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isPositionInSelectionRange(Player player, @NotNull BlockPos pos, ConfigOptionList selectionTypeConfig) {
        if (player == null || selectionTypeConfig == null) {
            return false;
        }
        if (!(selectionTypeConfig.getOptionListValue() instanceof SelectionType selectionType)) {
            return false;
        }
        return switch (selectionType) {
            case LITEMATICA_RENDER_LAYER -> DataManager.getRenderLayerRange().isPositionWithinRange(pos);
            case LITEMATICA_SELECTION_BELOW_PLAYER -> pos.getY() <= Math.floor(player.getY());
            case LITEMATICA_SELECTION_ABOVE_PLAYER -> pos.getY() >= Math.ceil(player.getY());
            default -> true;
        };
    }
}