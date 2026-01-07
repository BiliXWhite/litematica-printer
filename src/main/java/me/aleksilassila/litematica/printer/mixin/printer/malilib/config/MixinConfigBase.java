package me.aleksilassila.litematica.printer.mixin.printer.malilib.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigNotifiable;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.util.StringUtils;
import me.aleksilassila.litematica.printer.config.ConfigExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

// 对masa配置基类进行扩展, 进行I18n实现
@Mixin(value = ConfigBase.class)
public abstract class MixinConfigBase<T extends IConfigBase> implements IConfigBase, IConfigResettable, IConfigNotifiable<T>, ConfigExtension {
    @Shadow
    protected String prettyName;

    @Shadow
    protected String comment;

    @Unique
    private String litematica_printer$translateNameKey;

    @Unique
    private String litematica_printer$translateCommentKey;

    @Unique
    private BooleanSupplier litematica_printer$visible = litematica_printer$TRUE;

    @Inject(method = "getPrettyName", at = @At("HEAD"), cancellable = true)
    public void litematica_printer$getPrettyName(CallbackInfoReturnable<String> cir) {
        if (litematica_printer$translateNameKey != null && !litematica_printer$translateNameKey.isEmpty()) {
            cir.setReturnValue(StringUtils.getTranslatedOrFallback(litematica_printer$translateNameKey, prettyName));
        }
    }

    @Inject(method = "getComment", at = @At("HEAD"), cancellable = true)
    public void litematica_printer$getComment(CallbackInfoReturnable<String> cir) {
        @Nullable String comment = null;
        if (litematica_printer$translateCommentKey != null && !litematica_printer$translateCommentKey.isEmpty()) {
            comment = StringUtils.getTranslatedOrFallback(litematica_printer$translateCommentKey, null);
        }
        if (comment == null && litematica_printer$translateNameKey != null && !litematica_printer$translateNameKey.isEmpty()) {
            comment = StringUtils.getTranslatedOrFallback(litematica_printer$translateNameKey, null);
        }
        if (comment == null) {
            comment = this.comment;
        }
        if (comment != null) {
            cir.setReturnValue(comment);
        }
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

    @Override
    public @Nullable BooleanSupplier litematica_printer$getVisible() {
        return litematica_printer$visible;
    }

    @Override
    public void litematica_printer$setVisible(BooleanSupplier visible) {
        this.litematica_printer$visible = visible;
    }

    @Unique
    private final CopyOnWriteArrayList<Consumer<IConfigBase>> litematica_printer$valueChangeCallbacks = new CopyOnWriteArrayList<>();

    @Override
    public CopyOnWriteArrayList<Consumer<IConfigBase>> litematica_printer$getValueChangeCallbacks() {
        return litematica_printer$valueChangeCallbacks;
    }

    @Inject(method = "onValueChanged", at = @At("HEAD"))
    public void litematica_printer$onValueChanged(CallbackInfo ci) {
        if (!litematica_printer$valueChangeCallbacks.isEmpty()) {
            litematica_printer$triggerValueChangeCallbacks(this);
        }
    }
}
