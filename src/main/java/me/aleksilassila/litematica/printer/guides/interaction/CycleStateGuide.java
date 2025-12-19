package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CycleStateGuide extends InteractionGuide {
    private static final Property<?>[] propertiesToIgnore = new Property[]{
            BlockStateProperties.POWERED,
            BlockStateProperties.LIT,
    };

    public CycleStateGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!super.canExecute(player))
            return false;

        return targetState.getBlock() == currentState.getBlock();
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Collections.singletonList(ItemStack.EMPTY);
    }

    @Override
    protected boolean statesEqual(BlockState state1, BlockState state2) {
        if (state2.getBlock() instanceof LeverBlock) {
            return super.statesEqual(state1, state2);
        }

        return statesEqualIgnoreProperties(state1, state2, propertiesToIgnore);
    }
}
