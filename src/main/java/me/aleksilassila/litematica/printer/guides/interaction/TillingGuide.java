package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.guides.placement.FarmlandGuide;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class TillingGuide extends InteractionGuide {
    public static final Item[] HOE_ITEMS = new Item[]{
            Items.NETHERITE_HOE,
            Items.DIAMOND_HOE,
            Items.GOLDEN_HOE,
            Items.IRON_HOE,
            Items.STONE_HOE,
            Items.WOODEN_HOE
    };

    public TillingGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!super.canExecute(player))
            return false;

        return Arrays.stream(FarmlandGuide.TILLABLE_BLOCKS).anyMatch(b -> b == currentState.getBlock());
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Arrays.stream(HOE_ITEMS).map(ItemStack::new).toList();
    }
}
