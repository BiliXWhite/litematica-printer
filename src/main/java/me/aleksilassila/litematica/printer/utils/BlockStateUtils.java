package me.aleksilassila.litematica.printer.utils;

import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public class BlockStateUtils {
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
}
