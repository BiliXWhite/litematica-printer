package me.aleksilassila.litematica.printer.mixin.masa.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigNotifiable;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.util.StringUtils;
import me.aleksilassila.litematica.printer.config.ConfigBaseExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 对masa配置基类进行扩展, 进行I18n实现
@Mixin(ConfigBase.class)
public abstract class MixinIConfigBase<T extends IConfigBase> implements IConfigBase, IConfigResettable, IConfigNotifiable<T>, ConfigBaseExtension {
    @Shadow
    public abstract String getName();

    @Shadow
    public abstract String getPrettyName();

    @Unique
    private String litematica_printer$translateNameKey;

    @Unique
    private String litematica_printer$translateCommentKey;


    @Inject(method = "getPrettyName", at = @At("HEAD"), cancellable = true)
    public void litematica_printer$getPrettyName(CallbackInfoReturnable<String> cir) {
        if (litematica_printer$translateNameKey != null && !litematica_printer$translateNameKey.isEmpty()) {
            cir.setReturnValue(StringUtils.getTranslatedOrFallback(litematica_printer$translateNameKey, litematica_printer$translateCommentKey));
        }
    }

//    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
//    public void litematica_printer$getName(CallbackInfoReturnable<String> cir) {
//        if (litematica_printer$translateNameKey != null && !litematica_printer$translateNameKey.isEmpty()) {
//            cir.setReturnValue(StringUtils.getTranslatedOrFallback(litematica_printer$translateNameKey, name));
//        }
//    }

    @Inject(method = "getComment", at = @At("HEAD"), cancellable = true)
    public void litematica_getComment(CallbackInfoReturnable<String> cir) {
        if (litematica_printer$translateCommentKey != null && !litematica_printer$translateCommentKey.isEmpty()) {
            cir.setReturnValue(StringUtils.getTranslatedOrFallback(litematica_printer$translateCommentKey, litematica_printer$translateCommentKey));
        } else if (litematica_printer$translateNameKey != null && !litematica_printer$translateNameKey.isEmpty()) {
            cir.setReturnValue(StringUtils.getTranslatedOrFallback(litematica_printer$translateNameKey, litematica_printer$translateCommentKey));
        }
    }

    @Override
    public String getConfigGuiDisplayName() {
        return getPrettyName();
    }

    @Override
    public String litematica_printer$getTranslateNameKey() {
        return this.litematica_printer$translateNameKey;
    }

    @Override
    public void litematica_printer$setTranslateNameKey(String translateKey) {
        this.litematica_printer$translateNameKey = translateKey;
    }

    @Override
    public String litematica_printer$getTranslateCommentKey() {
        return this.litematica_printer$translateCommentKey;
    }

    @Override
    public void litematica_printer$setTranslateCommentKey(String translateKey) {
        this.litematica_printer$translateCommentKey = translateKey;
    }
}
