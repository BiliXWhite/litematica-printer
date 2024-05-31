package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC > 12001
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.api.memory.MemoryBankAccess;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;

@Mixin(MemoryBankAccessImpl.class)
public abstract class MemoryBankAccessMixin{

    @Shadow(remap = false)
    @Nullable
    private static MemoryBankImpl loaded = null;

    @Inject(at = @At("TAIL"), method = "loadOrCreate",remap = false)
    private void loadOrCreate(String id, String creationName, CallbackInfoReturnable<Boolean> cir) {
        String[] split = id.split("-");
        if ("printer".equals(split[split.length - 1])) {
            MemoryBankImpl loaded1 = loaded;
            MemoryBankImpl printerMemory = PRINTER_MEMORY;
            if(PRINTER_MEMORY != null) loaded = PRINTER_MEMORY;
        }
    }

    /**
     * @author zxy
     * @reason 原方法的无限并不是真正的无限
     */
//
//    @Inject(at = @At("HEAD"), method = "unload", cancellable = true,remap = false)
//    private static void unLoad(CallbackInfo ci) {
//        if(MemoryBank.INSTANCE == null) return;
//        String[] split = MemoryBank.INSTANCE.getId().split("-");
//        if ("printer".equals(split[split.length - 1])) {
//            ci.cancel();
//        }
//    }
}

//#endif