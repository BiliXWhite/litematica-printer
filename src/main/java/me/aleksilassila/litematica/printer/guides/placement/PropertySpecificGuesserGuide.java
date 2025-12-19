package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public class PropertySpecificGuesserGuide extends GuesserGuide {
    protected static Property<?>[] ignoredProperties = new Property[]{
            RepeaterBlock.DELAY,
            ComparatorBlock.MODE,
            RedStoneWireBlock.POWER,
            RedStoneWireBlock.EAST,
            RedStoneWireBlock.NORTH,
            RedStoneWireBlock.SOUTH,
            RedStoneWireBlock.WEST,
            BlockStateProperties.POWERED,
            BlockStateProperties.OPEN,
            PointedDripstoneBlock.THICKNESS,
            ScaffoldingBlock.DISTANCE,
            ScaffoldingBlock.BOTTOM,
            CactusBlock.AGE,
            BambooStalkBlock.AGE,
            BambooStalkBlock.LEAVES,
            BambooStalkBlock.STAGE,
            SaplingBlock.STAGE,
            CrossCollisionBlock.EAST,
            CrossCollisionBlock.NORTH,
            CrossCollisionBlock.SOUTH,
            CrossCollisionBlock.WEST,
            SnowLayerBlock.LAYERS,
            SeaPickleBlock.PICKLES,
            CandleBlock.CANDLES,
            EndPortalFrameBlock.HAS_EYE,
            BlockStateProperties.LIT,
            LeavesBlock.DISTANCE,
            LeavesBlock.PERSISTENT,
            BlockStateProperties.ATTACHED,
            BlockStateProperties.NOTE,
            BlockStateProperties.NOTEBLOCK_INSTRUMENT,

    };

    public PropertySpecificGuesserGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    protected boolean statesEqual(BlockState resultState, BlockState targetState) {
        return statesEqualIgnoreProperties(resultState, targetState, ignoredProperties);
    }
}
