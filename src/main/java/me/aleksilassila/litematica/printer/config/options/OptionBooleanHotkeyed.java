package me.aleksilassila.litematica.printer.config.options;

import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.MutableComponent;

public class OptionBooleanHotkeyed extends ConfigBooleanHotkeyed {
    private OptionBooleanHotkeyed(I18n i18n, boolean defaultValue, String defaultHotkey) {
        super(i18n.getConfigNameString(), defaultValue, defaultHotkey, i18n.getConfigCommentString());
    }

    private OptionBooleanHotkeyed(I18n i18n, boolean defaultValue, String defaultHotkey, String delimiter, MutableComponent... comments) {
        super(i18n.getConfigNameString(), defaultValue, defaultHotkey, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionBooleanHotkeyed create(I18n i18n, boolean defaultValue) {
        return new OptionBooleanHotkeyed(i18n, defaultValue, "");
    }

    public static OptionBooleanHotkeyed create(I18n i18n, boolean defaultValue, String defaultHotkey) {
        return new OptionBooleanHotkeyed(i18n, defaultValue, defaultHotkey);
    }

    public static OptionBooleanHotkeyed create(I18n i18n, boolean defaultValue, String defaultHotkey, String delimiter, MutableComponent... comments) {
        return new OptionBooleanHotkeyed(i18n, defaultValue, defaultHotkey, delimiter, comments);
    }

    public static OptionBooleanHotkeyed create(I18n i18n, boolean defaultValue, String defaultHotkey, MutableComponent... comments) {
        return new OptionBooleanHotkeyed(i18n, defaultValue, defaultHotkey, "", comments);
    }
}
