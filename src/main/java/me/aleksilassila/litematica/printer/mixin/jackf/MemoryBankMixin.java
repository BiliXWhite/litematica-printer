package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC > 12001
//$$ import net.minecraft.util.Identifier;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Position;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import red.jackf.chesttracker.memory.Memory;
//$$ import red.jackf.chesttracker.memory.MemoryBank;
//$$ import red.jackf.chesttracker.memory.metadata.Metadata;
//$$
//$$ import java.util.Map;
//$$
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;
//$$
//$$ @Mixin(MemoryBank.class)
//$$ public abstract class MemoryBankMixin {
//$$     protected MemoryBankMixin(Map<Identifier, Map<BlockPos, Memory>> memories) {
//$$         this.memories = memories;
//$$     }
//$$
//$$     @Inject(at = @At("TAIL"), method = "loadOrCreate",remap = false)
//$$     private static void loadOrCreate(String id, Metadata creationMetadata, CallbackInfo ci) {
//$$         String[] split = id.split("-");
//$$         if ("printer".equals(split[split.length - 1])) {
//$$             MemoryBank.INSTANCE = PRINTER_MEMORY;
//$$         }
//$$     }
//$$     @Mutable
//$$     @Final
//$$     @Shadow(remap = false)
//$$     private final Map<Identifier, Map<BlockPos, Memory>> memories;
//$$     @Shadow(remap = false)
//$$     private Metadata metadata;
//$$     /**
//$$      * @author zxy
//$$      * @reason 原方法的无限并不是真正的无限
//$$      */
//$$     @Redirect(at = @At(value = "INVOKE",target = "Lnet/minecraft/util/math/BlockPos;getSquaredDistance(Lnet/minecraft/util/math/Position;)D"),method = "getPositions")
//$$     private double getSquaredDistance(BlockPos instance, Position position){
//$$        // System.out.println(instance.getSquaredDistance(position));
//$$         return (metadata.getSearchSettings().searchRange == Integer.MAX_VALUE) ? -1 : instance.getSquaredDistance(position);
//$$     }
//$$ //    @Inject(at = @At("HEAD"), method = "unload", cancellable = true,remap = false)
//$$ //    private static void unLoad(CallbackInfo ci) {
//$$ //        if(MemoryBank.INSTANCE == null) return;
//$$ //        String[] split = MemoryBank.INSTANCE.getId().split("-");
//$$ //        if ("printer".equals(split[split.length - 1])) {
//$$ //            ci.cancel();
//$$ //        }
//$$ //    }
//$$ }
//$$
//#endif