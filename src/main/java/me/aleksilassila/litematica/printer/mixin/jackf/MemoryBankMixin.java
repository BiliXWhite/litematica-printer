package me.aleksilassila.litematica.printer.mixin.jackf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.chesttracker.memory.metadata.Metadata;

import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;

@Mixin(MemoryBank.class)
public abstract class MemoryBankMixin {
    @Inject(at = @At("TAIL"), method = "loadOrCreate",remap = false)
    private static void loadOrCreate(String id, Metadata creationMetadata, CallbackInfo ci) {
        String[] split = id.split("-");
        if ("printer".equals(split[split.length - 1])) {
            MemoryBank.INSTANCE = PRINTER_MEMORY;
        }
    }

//    @Inject(at = @At("HEAD"), method = "unload", cancellable = true,remap = false)
//    private static void unLoad(CallbackInfo ci) {
//        if(MemoryBank.INSTANCE == null) return;
//        String[] split = MemoryBank.INSTANCE.getId().split("-");
//        if ("printer".equals(split[split.length - 1])) {
//            ci.cancel();
//        }
//    }
}
