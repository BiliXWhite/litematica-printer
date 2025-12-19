package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class FarmlandGuide extends GeneralPlacementGuide {
    public static final Block[] TILLABLE_BLOCKS = new Block[]{
            Blocks.DIRT,
            Blocks.GRASS_BLOCK,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH,
    };

    public FarmlandGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    protected @NotNull List<ItemStack> getRequiredItems() {
        return Arrays.stream(TILLABLE_BLOCKS).map(b -> getBlockItem(b.defaultBlockState())).toList();
    }
}
