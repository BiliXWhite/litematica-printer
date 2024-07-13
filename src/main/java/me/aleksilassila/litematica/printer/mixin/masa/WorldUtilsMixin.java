package me.aleksilassila.litematica.printer.mixin.masa;

import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldUtils.class)
public class WorldUtilsMixin {
    @Invoker("applyBlockSlabProtocol")
    public static Vec3d applyBlockSlabProtocol(BlockPos pos, BlockState state, Vec3d hitVecIn) {
        return null;
    }

    @Inject(at = @At("HEAD"), method = "applyCarpetProtocolHitVec")
    private static void applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn, CallbackInfoReturnable<Vec3d> cir) {
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
        int id = 0;
        if (facing != null) {
            id = facing.getId();
        }

    }
}
