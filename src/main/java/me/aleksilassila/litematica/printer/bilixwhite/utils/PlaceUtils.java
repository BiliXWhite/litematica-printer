package me.aleksilassila.litematica.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.FillModeFacingType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

//#if MC >= 12109
//#else
//$$ import fi.dy.masa.litematica.util.WorldUtils;
//#endif

//#if MC >= 12105
//#endif


public class PlaceUtils {
    @NotNull
    static Minecraft client = Minecraft.getInstance();

    /**
     * 判断该方块是否需要水
     *
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterRequired(BlockState blockState) {
        return
                blockState.is(Blocks.WATER) &&
                        blockState.getValue(LiquidBlock.LEVEL) == 0 || (
                        blockState.getProperties().contains(BlockStateProperties.WATERLOGGED) &&
                                blockState.getValue(BlockStateProperties.WATERLOGGED)
                ) ||
                        blockState.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.is(Blocks.WATER)) return false;
        if (requiredState.is(Blocks.WATER) && currentState.getValue(LiquidBlock.LEVEL).equals(requiredState.getValue(LiquidBlock.LEVEL)))
            return true;
        else return currentState.getValue(LiquidBlock.LEVEL) == 0;
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
            };
        }
        return Direction.UP;
    }




    /**
     * 获取面向这个位置的侦测器的位置。
     * 该方法会检查给定位置周围的六个方向，查找是否有朝向相反方向的观察者方块。
     * 如果找到，则返回该观察者方块的位置；否则返回 null。
     *
     * @param pos   要检查的中心位置
     * @param world 当前的世界对象
     * @return 观察者方块的位置，如果未找到则返回 null
     */
    public static BlockPos getObserverPosition(BlockPos pos, Level world) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ObserverBlock) {
                Direction facing = neighborState.getValue(ObserverBlock.FACING);
                if (facing == direction.getOpposite()) {
                    return neighborPos;
                }
            }
        }
        return null;
    }


}
