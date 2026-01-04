package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import me.aleksilassila.litematica.printer.I18n;
import org.jetbrains.annotations.Nullable;

public class OptionListConfigBuilder extends BaseConfigBuilder<ConfigOptionList, OptionListConfigBuilder> {
    private IConfigOptionListEntry defaultValue;
    private @Nullable IHotkeyCallback keybindCallback;

    public OptionListConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public OptionListConfigBuilder defaultValue(IConfigOptionListEntry value) {
        this.defaultValue = value;
        return this;
    }

    public OptionListConfigBuilder keybindCallback(@Nullable IHotkeyCallback keybindCallback) {
        this.keybindCallback = keybindCallback;
        return this;
    }

    @Override
    public ConfigOptionList build() {
        ConfigOptionList config = new ConfigOptionList(i18n.getId(), defaultValue, commentKey);
        return buildExtension(config);
    }
}