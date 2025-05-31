package me.aleksilassila.litematica.printer.mixin.masa;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.aleksilassila.litematica.printer.LitematicaMixinMod.USE_EASY_MODE;
import static me.aleksilassila.litematica.printer.printer.Printer.easyPos;

@Mixin(WorldUtils.class)
public class WorldUtilsMixin {
    @Invoker("applyBlockSlabProtocol")
    public static Vec3d applyBlockSlabProtocol(BlockPos pos, BlockState state, Vec3d hitVecIn) {
        return null;
    }
    @WrapOperation(at= @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;getGenericTrace(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;DZZZ)Lfi/dy/masa/litematica/util/RayTraceUtils$RayTraceWrapper;"),method = "doEasyPlaceAction")
    private static RayTraceUtils.RayTraceWrapper doSchematicWorldPickBlock(World world, Entity dist2, double verifier, boolean posList, boolean traceMismatch, boolean worldClient, Operation<RayTraceUtils.RayTraceWrapper> original){
        if (USE_EASY_MODE.getBooleanValue() && easyPos != null) {
            return new RayTraceUtils.RayTraceWrapper(RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK,new BlockHitResult(Vec3d.ofCenter(easyPos),Direction.UP,easyPos,false));
        }else {
            return original.call(world, dist2, verifier, posList, traceMismatch, worldClient);
        }

    }
    @WrapOperation(at= @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;getFurthestSchematicWorldTraceBeforeVanilla(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;D)Lfi/dy/masa/litematica/util/RayTraceUtils$RayTraceWrapper;"),method = "doEasyPlaceAction")
    private static RayTraceUtils.RayTraceWrapper doSchematicWorldPickBlock2(World vanillaHitResult, Entity dist, double trace, Operation<RayTraceUtils.RayTraceWrapper> original){
        if (USE_EASY_MODE.getBooleanValue() && easyPos != null) {
            return new RayTraceUtils.RayTraceWrapper(RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK,new BlockHitResult(Vec3d.ofCenter(easyPos),Direction.UP,easyPos,false));
        }else {
            return original.call(vanillaHitResult, dist, trace);
        }
    }
}
