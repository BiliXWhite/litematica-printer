package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class FlowerPotFillGuide extends InteractionGuide {
    private final Block content;

    public FlowerPotFillGuide(SchematicBlockState state) {
        super(state);

        Block targetBlock = state.targetState.getBlock();
        if (targetBlock instanceof FlowerPotBlock flowerPotBlock) {
            this.content = flowerPotBlock.getPotted();
        } else {
            this.content = null;
        }
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (content == null)
            return false;
        if (!(currentState.getBlock() instanceof FlowerPotBlock))
            return false;

        return super.canExecute(player);
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        if (content == null)
            return Collections.emptyList();
        else
            return Collections.singletonList(new ItemStack(content));
    }
}
