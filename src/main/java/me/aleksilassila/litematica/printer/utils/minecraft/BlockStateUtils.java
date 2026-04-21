package me.aleksilassila.litematica.printer.utils.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

@SuppressWarnings("EnhancedSwitchMigration")
public class BlockStateUtils extends BlockUtils {
    private final static BooleanProperty wallUpProperty = WallBlock.UP;
    //#if MC > 12104
    private final static EnumProperty<WallSide> wallNorthProperty = WallBlock.NORTH;
    private final static EnumProperty<WallSide> wallSouthProperty = WallBlock.SOUTH;
    private final static EnumProperty<WallSide> wallWestProperty = WallBlock.WEST;
    private final static EnumProperty<WallSide> wallEastProperty = WallBlock.EAST;
    //#else
    //$$ private final static EnumProperty<WallSide> wallNorthProperty = WallBlock.NORTH_WALL;
    //$$ private final static EnumProperty<WallSide> wallSouthProperty = WallBlock.SOUTH_WALL;
    //$$ private final static EnumProperty<WallSide> wallWestProperty = WallBlock.WEST_WALL;
    //$$ private final static EnumProperty<WallSide> wallEastProperty = WallBlock.EAST_WALL;
    //#endif

    public static boolean statesEqualIgnoreProperties(BlockState state1, BlockState state2, Property<?>... propertiesToIgnore) {
        if (state1.getBlock() != state2.getBlock()) {
            return false;
        }
        loop:
        for (Property<?> property : state1.getProperties()) {
            if (property == BlockStateProperties.WATERLOGGED && !(state1.getBlock() instanceof CoralPlantBlock)) {
                continue;
            }
            for (Property<?> ignoredProperty : propertiesToIgnore) {
                if (property == ignoredProperty) {
                    continue loop;
                }
            }
            try {
                if (!state1.getValue(property).equals(state2.getValue(property))) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Comparable<T>> Optional<T> getProperty(BlockState blockState, Property<T> property) {
        if (blockState.hasProperty(property)) {
            return Optional.of(blockState.getValue(property));
        }
        return Optional.empty();
    }

    public static boolean statesEqual(BlockState state1, BlockState state2) {
        return statesEqualIgnoreProperties(state1, state2);
    }

    protected static boolean canBeClicked(Level world, BlockPos pos) {
        return getOutlineShape(world, pos) != Shapes.empty();
    }

    private static VoxelShape getOutlineShape(Level level, BlockPos pos) {
        return level.getBlockState(pos).getShape(level, pos);
    }

    private static VoxelShape getOutlineShape(BlockState state, Level level, BlockPos pos) {
        return state.getShape(level, pos);
    }

    public static Optional<Property<?>> getWallFacingProperty(Direction wallFacing) {
        switch (wallFacing) {
            case UP:
                return Optional.of(wallUpProperty);
            case NORTH:
                return Optional.of(wallNorthProperty);
            case SOUTH:
                return Optional.of(wallSouthProperty);
            case WEST:
                return Optional.of(wallWestProperty);
            case EAST:
                return Optional.of(wallEastProperty);
        }
        return Optional.empty();
    }

    public static Optional<Property<?>> getCrossCollisionBlock(Direction wallFacing) {
        switch (wallFacing) {
            case NORTH:
                return Optional.of(wallNorthProperty);
            case SOUTH:
                return Optional.of(wallSouthProperty);
            case WEST:
                return Optional.of(wallWestProperty);
            case EAST:
                return Optional.of(wallEastProperty);
        }
        return Optional.empty();
    }

    /**
     * 判断该方块是否需要水
     *
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterBlock(BlockState blockState) {
        return blockState.is(Blocks.WATER) && blockState.getValue(LiquidBlock.LEVEL) == 0
                || (blockState.getProperties().contains(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED))
                || blockState.getBlock() instanceof BubbleColumnBlock
                || blockState.getBlock() instanceof SeagrassBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.is(Blocks.WATER)) return false;
        if (requiredState.is(Blocks.WATER) && currentState.getValue(LiquidBlock.LEVEL).equals(requiredState.getValue(LiquidBlock.LEVEL))) {
            return true;
        } else {
            return currentState.getValue(LiquidBlock.LEVEL) == 0;
        }
    }
}
