package me.aleksilassila.litematica.printer.mixin.printer.litematica;

import org.spongepowered.asm.mixin.Mixin;

//#if MC >= 12109
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EasyPlaceUtils.class)
public interface EasyPlaceUtilsAccessor {
    @Invoker(remap = false)
    static void callSetEasyPlaceLastPickBlockTime() {
        throw new UnsupportedOperationException();
    }
}
//#else
//$$ import me.aleksilassila.litematica.printer.config.Pointless;
//$$
//$$ @Mixin(Pointless.class)
//$$ public interface EasyPlaceUtilsAccessor {}
//#endif