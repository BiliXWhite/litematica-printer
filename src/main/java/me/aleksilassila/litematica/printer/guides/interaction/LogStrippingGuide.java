package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LogStrippingGuide extends InteractionGuide {
    static final Item[] AXE_ITEMS = new Item[]{
            Items.NETHERITE_AXE,
            Items.DIAMOND_AXE,
            Items.GOLDEN_AXE,
            Items.IRON_AXE,
            Items.STONE_AXE,
            Items.WOODEN_AXE
    };

    public static final Map<Block, Block> STRIPPED_BLOCKS = AxeItemAccessor.getStrippedBlocks();

    public LogStrippingGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!InitHandler.STRIP_LOGS.getBooleanValue())
            return false;

        if (!super.canExecute(player))
            return false;

        Block strippingResult = STRIPPED_BLOCKS.get(currentState.getBlock());
        return strippingResult == targetState.getBlock();
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Arrays.stream(AXE_ITEMS).map(ItemStack::new).toList();
    }
}
