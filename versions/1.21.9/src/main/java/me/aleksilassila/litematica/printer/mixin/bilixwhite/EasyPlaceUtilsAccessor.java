package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import fi.dy.masa.litematica.util.EasyPlaceUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EasyPlaceUtils.class)
public interface EasyPlaceUtilsAccessor {
    @Invoker(remap = false)
    static void callSetEasyPlaceLastPickBlockTime() {
        throw new UnsupportedOperationException();
    }
}
