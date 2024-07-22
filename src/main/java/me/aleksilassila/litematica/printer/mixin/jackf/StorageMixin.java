package me.aleksilassila.litematica.printer.mixin.jackf;


//#if MC > 12001
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import red.jackf.chesttracker.impl.storage.Storage;
//$$
//$$ @Mixin(Storage.class)
//$$ public class StorageMixin {
//$$     //这方法啥意思？？？ 忘了
//$$     @Inject(at = @At("HEAD"), method = "delete",remap = false, cancellable = true)
//$$     private static void delete(String id, CallbackInfo ci) {
//$$ //        String[] split = id.split("-");
//$$ //        if("printer".equals(split[split.length-1])){
//$$ //            MemoryUtils.deletePrinterMemory();
//$$ //            ci.cancel();
//$$ //        }
//$$     }
//$$ }
//$$
//#endif