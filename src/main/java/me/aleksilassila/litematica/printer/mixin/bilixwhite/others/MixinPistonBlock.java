package me.aleksilassila.litematica.printer.mixin.bilixwhite.others;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public class MixinPistonBlock {
    @ModifyReturnValue(method = "getStateForPlacement", at = @At(value = "RETURN"))
    private BlockState fixStateForPlacement(BlockState blockState) {
        if (Printer.getInstance().pistonNeedFix) {
            Printer.getInstance().pistonNeedFix = false;
            //TODO: 检查这是否正确
            if (Printer.getInstance().requiredState.getBlock() instanceof PistonBaseBlock) {
                blockState = Printer.getInstance().requiredState.setValue(PistonBaseBlock.EXTENDED, false);
            }
        }
        return blockState;
    }
}