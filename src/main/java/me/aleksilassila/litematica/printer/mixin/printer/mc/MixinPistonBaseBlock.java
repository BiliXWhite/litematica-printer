package me.aleksilassila.litematica.printer.mixin.printer.mc;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.aleksilassila.litematica.printer.handler.Handlers;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public class MixinPistonBaseBlock {
//    @ModifyReturnValue(method = "getStateForPlacement", at = @At(value = "RETURN"))
//    private BlockState fixStateForPlacement(BlockState blockState) {
//        if (Handlers.PRINT.isPistonNeedFix()) {
//            printer.pistonNeedFix = false;
//            BlockContext ctx = printer.blockContext;
//            if (ctx != null && ctx.requiredState.getBlock() instanceof PistonBaseBlock) {
//                blockState = ctx.requiredState.setValue(PistonBaseBlock.EXTENDED, false);
//            }
//        }
//        return blockState;
//    }
}