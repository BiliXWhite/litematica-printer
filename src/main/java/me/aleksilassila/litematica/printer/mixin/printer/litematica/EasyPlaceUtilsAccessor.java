package me.aleksilassila.litematica.printer.mixin.printer.litematica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if MC > 12100
@Mixin(fi.dy.masa.litematica.util.EasyPlaceUtils.class)
//#else
//$$ @Mixin(fi.dy.masa.litematica.util.WorldUtils.class)
//#endif
public interface EasyPlaceUtilsAccessor {
    @Invoker("setEasyPlaceLastPickBlockTime")
    static void callSetEasyPlaceLastPickBlockTime() {
        throw new UnsupportedOperationException();
    }
}

