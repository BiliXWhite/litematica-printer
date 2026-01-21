package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.options.ConfigColor;
import me.aleksilassila.litematica.printer.I18n;

public class ColorConfigBuilder extends BaseConfigBuilder<ConfigColor, ColorConfigBuilder> {
    private String defaultValue = "";

    public ColorConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public ColorConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    public ColorConfigBuilder defaultValue(String value) {
        this.defaultValue = value;
        return this;
    }

    @Override
    public ConfigColor build() {
        ConfigColor config = new ConfigColor(i18n.getId(), defaultValue, commentKey);
        return buildExtension(config);
    }
}