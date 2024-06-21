package me.aleksilassila.litematica.printer.mixin.masa;

import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldUtils.class)
public interface WorldUtilsMixin {
    @Invoker("applyBlockSlabProtocol")
    public static Vec3d applyBlockSlabProtocol(BlockPos pos, BlockState state, Vec3d hitVecIn) {
        return null;
    }
}
