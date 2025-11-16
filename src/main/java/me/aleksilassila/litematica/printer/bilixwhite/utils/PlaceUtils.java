package me.aleksilassila.litematica.printer.bilixwhite.utils;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class PlaceUtils {
    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();

    /**
     * 判断该方块是否需要水
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterRequired(BlockState blockState) {
        return
            blockState.isOf(Blocks.WATER) &&
                    blockState.get(FluidBlock.LEVEL) == 0 || (
                blockState.getProperties().contains(Properties.WATERLOGGED) &&
                blockState.get(Properties.WATERLOGGED)
            ) ||
                blockState.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.isOf(Blocks.WATER)) return false;
        if (requiredState.isOf(Blocks.WATER) && currentState.get(FluidBlock.LEVEL).equals(requiredState.get(FluidBlock.LEVEL)))
            return true;
        else return currentState.get(FluidBlock.LEVEL) == 0;
    }

    public static boolean isReplaceable(BlockState state) {
        //#if MC < 11904
        //$$ return state.getMaterial().isReplaceable();
        //#else
        return state.isReplaceable();
        //#endif
    }

    public static Direction getFillModeFacing() {
        return switch (LitematicaPrinterMod.FILL_BLOCK_FACING.getOptionListValue().getStringValue()) {
            case "down" -> Direction.DOWN;
            case "east" -> Direction.EAST;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "north" -> Direction.NORTH;
            default -> Direction.UP;
        };
    }


    public static boolean canInteracted(BlockPos blockPos) {
        var range = LitematicaPrinterMod.PRINTER_RANGE.getIntegerValue();
        return switch (LitematicaPrinterMod.ITERATOR_SHAPE.getOptionListValue().getStringValue()) {
            case "sphere" -> canInteractedEuclidean(blockPos, range);
            case "octahedron" -> canInteractedManhattan(blockPos, range);
            default -> true;
        };
    }

    public static boolean canInteractedEuclidean(BlockPos blockPos, double range) {
        var player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= range * range;
    }

    public static boolean canInteractedManhattan(BlockPos pos, int range) {
        BlockPos center = client.player.getBlockPos();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }

    /**
     * 获取面向这个位置的侦测器的位置。
     * 该方法会检查给定位置周围的六个方向，查找是否有朝向相反方向的观察者方块。
     * 如果找到，则返回该观察者方块的位置；否则返回 null。
     *
     * @param pos 要检查的中心位置
     * @param world 当前的世界对象
     * @return 观察者方块的位置，如果未找到则返回 null
     */
    public static BlockPos getObserverPosition(BlockPos pos, World world) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ObserverBlock) {
                Direction facing = neighborState.get(ObserverBlock.FACING);
                if (facing == direction.getOpposite()) {
                    return neighborPos;
                }
            }
        }
        return null;
    }

    /**
     * 获取给定位置的侦测器的真侧面方块状态。
     * <p>
     * 如果给定的位置不是侦测器，则返回null
     *
     * @param pos 要检查的位置
     * @return 前方面块的状态组合
     */
    public static State getObverseFacingState(BlockPos pos) {
        BlockState requiredState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
        if (!(requiredState.getBlock() instanceof ObserverBlock)) return null;
        var obverseFacing = requiredState.get(Properties.FACING);
        var beObverseBlockSchematic = SchematicWorldHandler.getSchematicWorld().getBlockState(pos.offset(obverseFacing));
        var beObverseBlock = client.world.getBlockState(pos.offset(obverseFacing));
        return State.get(beObverseBlockSchematic, beObverseBlock);
    }
}
