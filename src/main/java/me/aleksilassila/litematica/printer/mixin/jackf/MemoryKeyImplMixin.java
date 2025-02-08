package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryKeyImpl;

@Mixin(MemoryKeyImpl.class)
public class MemoryKeyImplMixin {
    @WrapOperation(at = @At(value = "INVOKE",target = "Lnet/minecraft/util/math/BlockPos;getSquaredDistance(Lnet/minecraft/util/math/Position;)D"),method = "doSearch")
    public double doSearch(BlockPos instance, Position position, Operation<Double> original){
        return MemoryBankAccessImpl.INSTANCE.getLoadedInternal().map(memoryBank -> {
            return memoryBank.getMetadata().getSearchSettings().searchRange == Integer.MAX_VALUE ? -1 : instance.getSquaredDistance(position);
                }).orElse(-1.0);
    }
}
//#endif