package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBlock.class)
public class MixinPistonBlock {
    @ModifyReturnValue(method = "getPlacementState", at = @At(value = "RETURN"))
    private BlockState fixPlacementState(BlockState blockState) {
        if (PlacementGuide.pistonNeedFix) {
            PlacementGuide.pistonNeedFix = false;
            blockState = PlacementGuide.pistonState;
        }
        return blockState;
    }
}