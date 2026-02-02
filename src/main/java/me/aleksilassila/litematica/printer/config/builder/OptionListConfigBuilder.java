package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.I18n;

public class OptionListConfigBuilder extends BaseConfigBuilder<ConfigOptionList, OptionListConfigBuilder> {
    private IConfigOptionListEntry defaultValue;

    public OptionListConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public OptionListConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    public OptionListConfigBuilder defaultValue(IConfigOptionListEntry value) {
        this.defaultValue = value;
        return this;
    }

    @Override
    public ConfigOptionList build() {
        ConfigOptionList config = new ConfigOptionList(i18n.getNameKey(), defaultValue, descKey);
        return buildExtension(config);
    }
}