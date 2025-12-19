package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public class BlockIndifferentGuesserGuide extends GuesserGuide {
    public BlockIndifferentGuesserGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    protected boolean statesEqual(BlockState resultState, BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        Block resultBlock = resultState.getBlock();

        if (targetBlock instanceof BambooStalkBlock) {
            return resultBlock instanceof BambooStalkBlock || resultBlock instanceof BambooSaplingBlock;
        }

        if (targetBlock instanceof BigDripleafStemBlock) {
            if (resultBlock instanceof BigDripleafBlock || resultBlock instanceof BigDripleafStemBlock) {
                return resultState.getValue(HorizontalDirectionalBlock.FACING) == targetState.getValue(HorizontalDirectionalBlock.FACING);
            }
        }

        if (targetBlock instanceof TwistingVinesPlantBlock) {
            if (resultBlock instanceof TwistingVinesBlock) {
                return true;
            } else if (resultBlock instanceof TwistingVinesPlantBlock) {
                return statesEqualIgnoreProperties(resultState, targetState, TwistingVinesBlock.AGE);
            }
        }

        if (targetBlock instanceof TripWireBlock && resultBlock instanceof TripWireBlock) {
            return statesEqualIgnoreProperties(resultState, targetState,
                    TripWireBlock.ATTACHED, TripWireBlock.DISARMED, TripWireBlock.POWERED, TripWireBlock.NORTH,
                    TripWireBlock.EAST, TripWireBlock.SOUTH, TripWireBlock.WEST);
        }
        return super.statesEqual(resultState, targetState);
    }
}
