package me.aleksilassila.litematica.printer.mixin.printer.malilib.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBase;
import me.aleksilassila.litematica.printer.mixin_extension.ConfigExtension;
import me.aleksilassila.litematica.printer.utils.StringUtils;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 对masa配置基类进行扩展, 进行I18n实现
@Mixin(value = IConfigBase.class, remap = false)
public interface MixinIConfigBase {
    @Inject(method = "getConfigGuiDisplayName", at = @At("HEAD"), cancellable = true, remap = false)
    default void litematica_printer$getConfigGuiDisplayName(CallbackInfoReturnable<String> cir) {
        if (this instanceof ConfigBase<?> configBase) {
            if (configBase instanceof ConfigExtension extension) {
                if (extension.litematica_printer$getTranslateNameKey() != null) {
                    String translateKey = extension.litematica_printer$getTranslateNameKey();
                    cir.setReturnValue(StringUtils.translatable(translateKey).getString());
                }
            }
        }
    }
}
