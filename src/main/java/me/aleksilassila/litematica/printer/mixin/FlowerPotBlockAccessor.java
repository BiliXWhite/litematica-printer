package me.aleksilassila.litematica.printer.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.FlowerPotBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FlowerPotBlock.class)
public interface FlowerPotBlockAccessor {
    @Accessor("content")
    Block getContent();
}
