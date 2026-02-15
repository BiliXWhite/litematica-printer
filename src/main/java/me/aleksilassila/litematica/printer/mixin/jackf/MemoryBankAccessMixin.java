package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;

@Mixin(value = MemoryBankAccessImpl.class, remap = false)
public abstract class MemoryBankAccessMixin{
    @Shadow(remap = false)
    @Nullable
    private static MemoryBankImpl loaded = null;

    @Inject(at = @At("TAIL"), method = "loadOrCreate",remap = false)
    private void loadOrCreate(String id, String creationName, CallbackInfoReturnable<Boolean> cir) {
        String[] split = id.split("-");
        if ("printer".equals(split[split.length - 1])) {
            if(PRINTER_MEMORY != null) loaded = PRINTER_MEMORY;
        }
    }
}

//#else
//$$ import me.aleksilassila.litematica.printer.mixin_extension.Pointless;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ @Mixin(value = Pointless.class)
//$$ public class MemoryBankAccessMixin { }
//#endif