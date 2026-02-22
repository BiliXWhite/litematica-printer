package me.aleksilassila.litematica.printer.mixin.printer.mc;

import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PistonBaseBlock.class)
public class MixinPistonBaseBlock {
//    @ModifyReturnValue(method = "getStateForPlacement", at = @At(value = "RETURN"))
//    private BlockState fixStateForPlacement(BlockState blockState) {
//        if (ClientPlayerTickManager.PRINT.isPistonNeedFix()) {
//            printer.pistonNeedFix = false;
//            SchematicBlockContext ctx = printer.blockContext;
//            if (ctx != null && ctx.requiredState.getBlock() instanceof PistonBaseBlock) {
//                blockState = ctx.requiredState.setValue(PistonBaseBlock.EXTENDED, false);
//            }
//        }
//        return blockState;
//    }
}