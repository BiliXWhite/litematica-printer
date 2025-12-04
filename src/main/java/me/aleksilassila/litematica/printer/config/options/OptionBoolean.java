package me.aleksilassila.litematica.printer.config.options;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.Component;

public class OptionBoolean extends ConfigBoolean {
    private OptionBoolean(I18n i18n, boolean defaultValue) {
        super(i18n.getConfigNameString(), defaultValue, i18n.getConfigCommentString());
    }

    private OptionBoolean(I18n i18n, boolean defaultValue, String delimiter, Component... comments) {
        super(i18n.getConfigNameString(), defaultValue, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionBoolean create(I18n i18n, boolean defaultValue) {
        return new OptionBoolean(i18n, defaultValue);
    }

    public static OptionBoolean create(I18n i18n, boolean defaultValue, String delimiter, Component... comments) {
        return new OptionBoolean(i18n, defaultValue, delimiter, comments);
    }

    public static OptionBoolean create(I18n i18n, boolean defaultValue, Component... comments) {
        return new OptionBoolean(i18n, defaultValue, "", comments);
    }
}
