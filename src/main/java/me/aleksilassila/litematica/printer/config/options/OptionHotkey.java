package me.aleksilassila.litematica.printer.config.options;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.MutableComponent;

public class OptionHotkey extends ConfigHotkey {
    private OptionHotkey(I18n i18n, String defaultStorageString) {
        super(i18n.getConfigNameString(), defaultStorageString, KeybindSettings.DEFAULT, StringUtils.EMPTY.getString());
    }

    private OptionHotkey(I18n i18n, String defaultStorageString, KeybindSettings settings) {
        super(i18n.getConfigNameString(), defaultStorageString, settings, StringUtils.EMPTY.getString());
    }

    private OptionHotkey(I18n i18n, String defaultStorageString, KeybindSettings settings, String delimiter, MutableComponent... comments) {
        super(i18n.getConfigNameString(), defaultStorageString, settings, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionHotkey create(I18n i18n, String defaultValue) {
        return new OptionHotkey(i18n, defaultValue);
    }

    public static OptionHotkey create(I18n i18n) {
        return new OptionHotkey(i18n, "");
    }

    public static OptionHotkey create(I18n i18n, String defaultValue, KeybindSettings settings) {
        return new OptionHotkey(i18n, defaultValue, settings);
    }

    public static OptionHotkey create(I18n i18n, KeybindSettings settings) {
        return new OptionHotkey(i18n, "", settings);
    }

    public static OptionHotkey create(I18n i18n, KeybindSettings settings, String delimiter, MutableComponent... comments) {
        return new OptionHotkey(i18n, "", settings, delimiter, comments);
    }

    public static OptionHotkey create(I18n i18n, KeybindSettings settings, MutableComponent... comments) {
        return new OptionHotkey(i18n, "", settings, "", comments);
    }

    public static OptionHotkey create(I18n i18n, String defaultValue, KeybindSettings settings, String delimiter, MutableComponent... comments) {
        return new OptionHotkey(i18n, defaultValue, settings, delimiter, comments);
    }

    public static OptionHotkey create(I18n i18n, String defaultValue, KeybindSettings settings, MutableComponent... comments) {
        return new OptionHotkey(i18n, defaultValue, settings, "", comments);
    }
}
