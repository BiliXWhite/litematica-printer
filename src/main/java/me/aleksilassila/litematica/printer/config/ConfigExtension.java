package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBase;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BooleanSupplier;

public interface ConfigExtension {
    @Unique
    BooleanSupplier litematica_printer$TRUE = () -> true;

    @Unique
    BooleanSupplier litematica_printer$FALSE = () -> false;

    @Nullable String litematica_printer$getTranslateNameKey();

    void litematica_printer$setTranslateNameKey(@Nullable String translateKey);

    @Nullable String litematica_printer$getTranslateCommentKey();

    void litematica_printer$setTranslateCommentKey(@Nullable String translateKey);

    @Nullable BooleanSupplier litematica_printer$getVisible();

    void litematica_printer$setVisible(@Nullable BooleanSupplier visible);

    default void litematica_printer$setVisible(boolean visible) {
        this.litematica_printer$setVisible(visible ? litematica_printer$TRUE : litematica_printer$FALSE);
    }

    static BooleanSupplier litematica_printer$getVisible(ConfigExtension configExtension) {
        return configExtension.litematica_printer$getVisible();
    }

    static <T extends IConfigBase> void litematica_printer$setVisible(ConfigBase<T> config, BooleanSupplier visible) {
        if (config instanceof ConfigExtension configExtension) {
            configExtension.litematica_printer$setVisible(visible);
        }
    }

    static <T extends IConfigBase> void litematica_printer$setVisible(ConfigBase<T> config, boolean visible) {
        if (config instanceof ConfigExtension configExtension) {
            configExtension.litematica_printer$setVisible(visible);
        }
    }
}
