package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBlock.class)
public class MixinPistonBlock {
    @ModifyReturnValue(method = "getPlacementState", at = @At(value = "RETURN"))
    private BlockState fixPlacementState(BlockState blockState) {
        if (Printer.pistonNeedFix) {
            Printer.pistonNeedFix = false;
            blockState = Printer.requiredState.with(PistonBlock.EXTENDED, false);
        }
        return blockState;
    }
}