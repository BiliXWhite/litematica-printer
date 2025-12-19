package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TorchGuide extends GeneralPlacementGuide {
    public TorchGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    protected List<Direction> getPossibleSides() {
        Optional<Direction> facing = getProperty(targetState, HorizontalDirectionalBlock.FACING);

        return facing
                .map(direction -> Collections.singletonList(direction.getOpposite()))
                .orElseGet(() -> Collections.singletonList(Direction.DOWN));
    }

    @Override
    protected Optional<Block> getRequiredItemAsBlock(LocalPlayer player) {
        return Optional.of(state.targetState.getBlock());
    }
}
