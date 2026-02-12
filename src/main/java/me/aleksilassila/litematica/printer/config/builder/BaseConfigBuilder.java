package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBase;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.mixin_extension.ConfigExtension;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public abstract class BaseConfigBuilder<T extends ConfigBase<?>, B extends BaseConfigBuilder<T, B>> {
    protected final I18n i18n;
    protected String nameKey;
    protected String descKey;
    protected @Nullable BooleanSupplier visible;
    protected final CopyOnWriteArrayList<Consumer<IConfigBase>> valueChangeCallbacks = new CopyOnWriteArrayList<>();

    public BaseConfigBuilder(I18n i18n) {
        this.i18n = i18n;
        this.nameKey = i18n.getConfigNameKey();
        this.descKey = i18n.getConfigDescKey();
        this.visible = null;
    }

    public BaseConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    @SuppressWarnings("unchecked")
    public B setNameKey(String name) {
        this.nameKey = name;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setDescKey(String comment) {
        this.descKey = comment;
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

    @SuppressWarnings("unchecked")
    public B addValueChangeListener(Consumer<IConfigBase> callback) {
        if (callback != null) {
            this.valueChangeCallbacks.add(callback);
        }
        return (B) this;
    }

    protected T buildExtension(T config) {
        buildI18n(config);
        buildVisible(config);
        buildValueChangeCallbacks(config);
        return config;
    }

    protected void buildI18n(T config) {
        ConfigExtension extension = (ConfigExtension) config;
        extension.litematica_printer$setTranslateNameKey(this.nameKey);
        extension.litematica_printer$setTranslateCommentKey(this.descKey);
    }

    protected void buildVisible(T config) {
        if (visible != null) {
            ConfigExtension extension = (ConfigExtension) config;
            extension.litematica_printer$setVisible(this.visible);
        }
    }

    protected void buildValueChangeCallbacks(T config) {
        if (config instanceof ConfigExtension extension && !this.valueChangeCallbacks.isEmpty()) {
            for (Consumer<IConfigBase> callback : this.valueChangeCallbacks) {
                extension.litematica_printer$addValueChangeListener(callback);
            }
        }
    }

    public abstract T build();
}