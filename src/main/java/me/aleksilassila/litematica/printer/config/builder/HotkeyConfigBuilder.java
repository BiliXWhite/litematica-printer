package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.HotkeysCallback;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import org.jetbrains.annotations.Nullable;

public class HotkeyConfigBuilder extends BaseConfigBuilder<ConfigHotkey, HotkeyConfigBuilder> {
    private String defaultStorageString = "";
    private KeybindSettings keybindSettings = KeybindSettings.DEFAULT;
    private @Nullable IHotkeyCallback keybindCallback;
    private @Nullable ConfigOptionList bindConfig;

    public HotkeyConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public HotkeyConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    public HotkeyConfigBuilder defaultStorageString(String storageString) {
        this.defaultStorageString = storageString;
        return this;
    }

    public HotkeyConfigBuilder keybindCallback(@Nullable IHotkeyCallback keybindCallback) {
        this.keybindCallback = keybindCallback;
        return this;
    }

    public HotkeyConfigBuilder bindConfig(@Nullable ConfigOptionList bindConfigOptionList) {
        this.bindConfig = bindConfigOptionList;
        return this;
    }

    public HotkeyConfigBuilder keybindSettings(KeybindSettings settings) {
        this.keybindSettings = settings;
        return this;
    }

    @Override
    public ConfigHotkey build() {
        ConfigHotkey config = new ConfigHotkey(i18n.getNameKey(), defaultStorageString, keybindSettings, descKey);
        if (keybindCallback == null) {
            if (bindConfig == null) {
                keybindCallback = HotkeysCallback::onKeyAction;
            } else {
                keybindCallback = (action, key) -> onKeyAction(bindConfig);
            }
        }
        config.getKeybind().setCallback(keybindCallback);
        return buildExtension(config);
    }

    private boolean onKeyAction(ConfigOptionList config) {
        IConfigOptionListEntry cycle = config.getOptionListValue().cycle(true);
        config.setOptionListValue(cycle);
        MessageUtils.setOverlayMessage(config.getOptionListValue().getDisplayName());
        return true;
    }

}