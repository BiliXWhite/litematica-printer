package me.aleksilassila.litematica.printer.mixin.masa.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBase;
import me.aleksilassila.litematica.printer.config.ConfigBaseExtension;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 对masa配置基类进行扩展, 进行I18n实现
@Mixin(value = IConfigBase.class, remap = false)
public interface MixinIConfigBase {
    @Inject(method = "getConfigGuiDisplayName", at = @At("HEAD"), cancellable = true, remap = false)
    default void litematica_printer$getConfigGuiDisplayName(CallbackInfoReturnable<String> cir) {
        ConfigBase<?> configBase = (ConfigBase<?>) this;
        ConfigBaseExtension extension = (ConfigBaseExtension) configBase;
        if (extension.litematica_printer$getTranslateNameKey() != null) {
            cir.setReturnValue(configBase.getPrettyName());
        }
    }
}
