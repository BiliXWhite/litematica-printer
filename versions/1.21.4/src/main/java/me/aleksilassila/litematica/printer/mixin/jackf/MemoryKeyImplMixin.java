package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryKeyImpl;

@Mixin(MemoryKeyImpl.class)
public class MemoryKeyImplMixin {
    @WrapOperation(at = @At(value = "INVOKE",target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"),method = "doSearch")
    public double doSearch(BlockPos instance, Position position, Operation<Double> original){
        // 获取当前加载的记忆库，如果搜索范围设置为最大值(Integer.MAX_VALUE)则返回-1表示无限制搜索
        // 否则计算方块位置到指定坐标的距离平方值
        return MemoryBankAccessImpl.INSTANCE.getLoadedInternal()
            .map(memoryBank -> {
                int searchRange = memoryBank.getMetadata().getSearchSettings().searchRange;
                return searchRange == Integer.MAX_VALUE ? -1 : instance.distToCenterSqr(position);
            })
            .orElse(-1.0); // 如果没有加载记忆库，返回默认值-1.0
    }
}
//#endif