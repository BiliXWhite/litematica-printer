package me.aleksilassila.litematica.printer.config.builder;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import me.aleksilassila.litematica.printer.I18n;

public class StringListConfigBuilder extends BaseConfigBuilder<ConfigStringList, StringListConfigBuilder> {
    private ImmutableList<String> defaultValue = ImmutableList.of();

    public StringListConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public StringListConfigBuilder defaultValue(ImmutableList<String> value) {
        this.defaultValue = value;
        return this;
    }

    @Override
    public ConfigStringList build() {
        ConfigStringList config = new ConfigStringList(i18n.getId(), defaultValue, commentKey);
        return buildExtension(config);
    }
}