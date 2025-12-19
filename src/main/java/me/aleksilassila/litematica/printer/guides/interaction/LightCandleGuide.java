package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class LightCandleGuide extends InteractionGuide {
    boolean shouldBeLit;
    boolean isLit;

    public LightCandleGuide(SchematicBlockState state) {
        super(state);

        shouldBeLit = getProperty(targetState, BlockStateProperties.LIT).orElse(false);
        isLit = getProperty(currentState, BlockStateProperties.LIT).orElse(false);
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Collections.singletonList(new ItemStack(Items.FLINT_AND_STEEL));
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!super.canExecute(player))
            return false;

        return (currentState.getBlock() instanceof AbstractCandleBlock) && shouldBeLit && !isLit;
    }
}
