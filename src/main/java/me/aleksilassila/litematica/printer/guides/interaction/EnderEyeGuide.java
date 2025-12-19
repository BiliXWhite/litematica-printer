package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class EnderEyeGuide extends InteractionGuide {
    public EnderEyeGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!super.canExecute(player))
            return false;
        if (currentState.hasProperty(BlockStateProperties.EYE) && targetState.hasProperty(BlockStateProperties.EYE)) {
            return !currentState.getValue(BlockStateProperties.EYE) && targetState.getValue(BlockStateProperties.EYE);
        }
        return false;
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Collections.singletonList(new ItemStack(Items.ENDER_EYE));
    }
}
