package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.ModeType;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.config.enums.RadiusShapeType;
import me.aleksilassila.litematica.printer.config.enums.SelectionType;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//#if MC < 11900
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif

public class PrinterUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();

    public static Direction[] horizontalDirections = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static Direction getHalf(Half half) {
        return half == Half.TOP ? Direction.UP : Direction.DOWN;
    }

    public static Direction axisToDirection(Direction.Axis axis) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == axis) return direction;
        }
        return Direction.DOWN;
    }

    public static Comparable<?> getPropertyByName(BlockState state, String name) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase(name)) {
                return state.getValue(prop);
            }
        }
        return null;
    }

    public static boolean canBeClicked(ClientLevel world, BlockPos pos) {
        return getOutlineShape(world, pos) != Shapes.empty();
    }

    public static VoxelShape getOutlineShape(ClientLevel world, BlockPos pos) {
        return world.getBlockState(pos).getShape(world, pos);
    }

    public static Map<Direction, Vec3> getSlabSides(Level world, BlockPos pos, SlabType requiredHalf) {
        if (requiredHalf == SlabType.DOUBLE) requiredHalf = SlabType.BOTTOM;
        Direction requiredDir = requiredHalf == SlabType.TOP ? Direction.UP : Direction.DOWN;
        Map<Direction, Vec3> sides = new HashMap<>();
        sides.put(requiredDir, new Vec3(0, 0, 0));
        if (world.getBlockState(pos).hasProperty(SlabBlock.TYPE)) {
            sides.put(requiredDir.getOpposite(), Vec3.atLowerCornerOf(DirectionUtils.getVector(requiredDir)).scale(0.5));
        }
        for (Direction side : horizontalDirections) {
            BlockState neighborCurrentState = world.getBlockState(pos.relative(side));
            if (neighborCurrentState.hasProperty(SlabBlock.TYPE) && neighborCurrentState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) {
                if (neighborCurrentState.getValue(SlabBlock.TYPE) != requiredHalf) {
                    continue;
                }
            }
            sides.put(side, Vec3.atLowerCornerOf(DirectionUtils.getVector(requiredDir)).scale(0.25));
        }
        return sides;
    }

    public static boolean isPositionInSelectionRange(Player player, BlockPos pos, ConfigOptionList selectionTypeConfig) {
        if (player == null || pos == null || selectionTypeConfig == null) {
            return false;
        }
        if (!(selectionTypeConfig.getOptionListValue() instanceof SelectionType selectionType)) {
            return false;
        }
        return switch (selectionType) {
            // 投影渲染层：坐标在渲染层范围内 → 返回true
            case LITEMATICA_RENDER_LAYER -> DataManager.getRenderLayerRange().isPositionWithinRange(pos);
            // 玩家之下：坐标Y ≤ 玩家Y → 返回true
            case LITEMATICA_SELECTION_BELOW_PLAYER -> pos.getY() <= Math.floor(player.getY());
            // 玩家之上：坐标Y ≥ 玩家Y → 返回true
            case LITEMATICA_SELECTION_ABOVE_PLAYER -> pos.getY() >= Math.ceil(player.getY());
            // 默认（投影完整选区）：所有坐标都有效 → 返回true
            default -> true;
        };
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

    public static double getWorkRange() {
        double workRange = Configs.General.PRINTER_RANGE.getIntegerValue();
        if (Configs.General.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()) {
            return Math.min(workRange, PlayerUtils.getPlayerBlockInteractionRange());
        }
        return workRange;
    }

    // 判断是否可交互
    public static boolean canInteracted(BlockPos blockPos) {
        double workRange = getWorkRange();
        if (Configs.General.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()) {
            if (client.player != null && !PlayerUtils.canInteractWithBlockAt(client.player, blockPos, 1F)) {
                return false;
            }
        }
        if (Configs.General.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType radiusShapeType) {
            return switch (radiusShapeType) {
                case SPHERE -> PlayerUtils.canInteractedEuclidean(blockPos, workRange);
                case OCTAHEDRON -> PlayerUtils.canInteractedManhattan(blockPos, workRange);
                case CUBE -> PlayerUtils.canInteractedCube(blockPos, workRange);
            };
        }
        return true;
    }

    public static boolean isEnable() {
        return Configs.General.PRINT_SWITCH.getBooleanValue();
    }

    public static boolean isPrinterMode() {
        return Configs.General.MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI)
                || Configs.General.PRINTER_MODE.getOptionListValue() == PrintModeType.PRINTER;
    }

    public static boolean isMineMode() {
        return (Configs.General.MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI) && Configs.Excavate.MINE.getBooleanValue())
                || Configs.General.PRINTER_MODE.getOptionListValue() == PrintModeType.MINE;
    }

    public static boolean isFillMode() {
        return (Configs.General.MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI) && Configs.Fill.FILL.getBooleanValue())
                || Configs.General.PRINTER_MODE.getOptionListValue() == PrintModeType.FILL;
    }

    public static boolean isFluidMode() {
        return (Configs.General.MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI) && Configs.Hotkeys.FLUID.getBooleanValue())
                || Configs.General.PRINTER_MODE.getOptionListValue() == PrintModeType.FLUID;
    }

    public static boolean isBedrockMode() {
        return (Configs.General.MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI) && Configs.Hotkeys.BEDROCK.getBooleanValue())
                || Configs.General.PRINTER_MODE.getOptionListValue() == PrintModeType.BEDROCK;
    }
}
