package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.*;

public class RailGuesserGuide extends GuesserGuide {
    static final RailShape[] STRAIGHT_RAIL_SHAPES = new RailShape[]{
            RailShape.NORTH_SOUTH,
            RailShape.EAST_WEST
    };

    public RailGuesserGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    public boolean skipOtherGuides() {
        return true;
    }

    @Override
    protected boolean statesEqual(BlockState resultState, BlockState targetState) {
        if (!wouldConnectCorrectly())
            return false;
        // if (wouldBlockAnotherConnection()) return false;
        /*
         * TODO: Fully working rail guesser
         * If has a neighbor that:
         * - Has not been placed yet
         * - OR Has been placed but can change shape
         * - AND this placement should connect to only one rail, that is not the
         * neighbor
         * Then return false
         */

        if (getRailShape(resultState).isPresent()) {
            if (Arrays.stream(STRAIGHT_RAIL_SHAPES)
                    .anyMatch(shape -> shape == getRailShape(resultState).orElse(null))) {
                return super.statesEqualIgnoreProperties(resultState, targetState, BlockStateProperties.RAIL_SHAPE,
                        BlockStateProperties.RAIL_SHAPE_STRAIGHT, BlockStateProperties.POWERED);
            }
        }

        return super.statesEqual(resultState, targetState);
    }

    private boolean wouldConnectCorrectly() {
        RailShape targetShape = getRailShape(state.targetState).orElse(null);
        if (targetShape == null)
            return false;

        List<Direction> allowedConnections = getRailDirections(targetShape);

        List<Direction> possibleConnections = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (d.getAxis().isVertical())
                continue;
            SchematicBlockState neighbor = state.offset(d);

            if (hasFreeConnections(neighbor)) {
                possibleConnections.add(d);
            }
        }

        if (possibleConnections.size() > 2)
            return false;

        return new HashSet<>(allowedConnections).containsAll(possibleConnections);
    }

//    private boolean wouldBlockAnotherConnection() {
//        List<Direction> possibleConnections = new ArrayList<>();
//
//        for (Direction d : Direction.values()) {
//            if (d.getAxis().isVertical()) continue;
//            SchematicBlockState neighbor = state.offset(d);
//
//            if (couldConnectWrongly(neighbor)) {
//                possibleConnections.add(d);
//            }
//        }
//
//        return possibleConnections.size() > 1;
//    }

    private boolean hasFreeConnections(SchematicBlockState state) {
        List<Direction> possibleConnections = getRailDirections(state);
        if (possibleConnections.isEmpty())
            return false;

        for (Direction d : possibleConnections) {
            SchematicBlockState neighbor = state.offset(d);
            // FIXME -->  when will this ever not be equal? <.<
            if (neighbor.currentState.getBlock() != neighbor.currentState.getBlock()) {
                return false;
            }
        }

        return possibleConnections.stream().anyMatch(possibleDirection -> {
            SchematicBlockState neighbor = state.offset(possibleDirection);
            return !getRailDirections(neighbor).contains(possibleDirection.getOpposite());
        });
    }

    private List<Direction> getRailDirections(SchematicBlockState state) {
        RailShape shape = getRailShape(state.currentState).orElse(null);
        if (shape == null)
            return new ArrayList<>();

        return getRailDirections(shape);
    }

    private List<Direction> getRailDirections(RailShape railShape) {
        String name = railShape.getName();

        if (railShape.isSlope()) {
            Direction d = Direction.valueOf(name.replace("ascending_", "").toUpperCase());
            return Arrays.asList(d, d.getOpposite());
        } else {
            Direction d1 = Direction.valueOf(name.split("_")[0].toUpperCase());
            Direction d2 = Direction.valueOf(name.split("_")[1].toUpperCase());
            return Arrays.asList(d1, d2);
        }
    }

    Optional<RailShape> getRailShape(BlockState state) {
        Optional<RailShape> shape = getProperty(state, BlockStateProperties.RAIL_SHAPE);
        if (shape.isEmpty())
            return getProperty(state, BlockStateProperties.RAIL_SHAPE_STRAIGHT);
        return shape;
    }
}
