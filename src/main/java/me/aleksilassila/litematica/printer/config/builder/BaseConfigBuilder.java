package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.options.ConfigBase;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigExtension;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public abstract class BaseConfigBuilder<T extends ConfigBase<?>, B extends BaseConfigBuilder<T, B>> {
    protected final I18n i18n;
    protected String nameKey;
    protected String commentKey;
    protected @Nullable BooleanSupplier visible;

    public BaseConfigBuilder(I18n i18n) {
        this.i18n = i18n;
        this.nameKey = i18n.getConfigNameKey();
        this.commentKey = i18n.getConfigCommentKey();
        this.visible = null;
    }

    @SuppressWarnings("unchecked")
    public B setNameKey(String name) {
        this.nameKey = name;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setCommentKey(String comment) {
        this.commentKey = comment;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setVisible(boolean visible) {
        this.visible = visible ? ConfigExtension.litematica_printer$TRUE : ConfigExtension.litematica_printer$FALSE;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setVisible(@Nullable BooleanSupplier visible) {
        this.visible = visible;
        return (B) this;
    }

    protected T buildExtension(T config) {
        buildI18n(config);
        buildVisible(config);
        return config;
    }

    protected void buildI18n(T config) {
        ConfigExtension extension = (ConfigExtension) config;
        extension.litematica_printer$setTranslateNameKey(this.nameKey);
        extension.litematica_printer$setTranslateCommentKey(this.commentKey);
    }

    protected void buildVisible(T config) {
        if (visible != null) {
            ConfigExtension extension = (ConfigExtension) config;
            extension.litematica_printer$setVisible(this.visible);
        }
    }

    public abstract T build();
}