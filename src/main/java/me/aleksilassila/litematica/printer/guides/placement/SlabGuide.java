package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SlabGuide extends GeneralPlacementGuide {
    public SlabGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    protected List<Direction> getPossibleSides() {
        List<Direction> resultList = new ArrayList<>();
        SlabType targetSlabType = getProperty(state.targetState, SlabBlock.TYPE).orElse(SlabType.DOUBLE);

        if (targetSlabType == SlabType.DOUBLE) {
            return super.getPossibleSides();
        }

        Direction[] directionsToCheck = {
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };

        for (Direction direction : directionsToCheck) {
            SlabType neighborSlabType = getProperty(state.offset(direction).currentState, SlabBlock.TYPE).orElse(SlabType.DOUBLE);

            if (neighborSlabType == SlabType.DOUBLE || neighborSlabType == targetSlabType) {
                resultList.add(direction);
            }
        }

        if (targetSlabType == SlabType.TOP || targetSlabType == SlabType.BOTTOM) {
            Direction verticalDirection = targetSlabType == SlabType.TOP ? Direction.UP : Direction.DOWN;
            SlabType neighborSlabType = getProperty(state.offset(verticalDirection).currentState, SlabBlock.TYPE).orElse(SlabType.DOUBLE);

            if (neighborSlabType == SlabType.DOUBLE || neighborSlabType != targetSlabType) {
                resultList.add(verticalDirection);
            }
        }

        return resultList;
    }

    @Override
    protected Vec3 getHitModifier(Direction validSide) {
        Direction requiredHalf = getRequiredHalf(state);
        if (validSide.get2DDataValue() != -1) {
            return new Vec3(0, requiredHalf.getStepY() * 0.25, 0);
        } else {
            return new Vec3(0, 0, 0);
        }
    }

    private Direction getRequiredHalf(SchematicBlockState state) {
        BlockState targetState = state.targetState;
        BlockState currentState = state.currentState;

        if (!currentState.hasProperty(SlabBlock.TYPE)) {
            return targetState.getValue(SlabBlock.TYPE) == SlabType.TOP ? Direction.UP : Direction.DOWN;
        } else if (currentState.getValue(SlabBlock.TYPE) != targetState.getValue(SlabBlock.TYPE)) {
            return currentState.getValue(SlabBlock.TYPE) == SlabType.TOP ? Direction.DOWN : Direction.UP;
        } else {
            return Direction.DOWN;
        }
    }
}
