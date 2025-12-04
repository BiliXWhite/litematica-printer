package me.aleksilassila.litematica.printer.config.options;

import fi.dy.masa.malilib.config.options.ConfigColor;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.MutableComponent;

public class OptionColor extends ConfigColor {
    private OptionColor(I18n i18n, String defaultValue) {
        super(i18n.getConfigNameString(), defaultValue, i18n.getConfigCommentString());
    }

    private OptionColor(I18n i18n, String defaultValue, String delimiter, MutableComponent... comments) {
        super(i18n.getConfigNameString(), defaultValue, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionColor create(I18n i18n, String defaultValue) {
        return new OptionColor(i18n, defaultValue);
    }

    public static OptionColor create(I18n i18n, String defaultValue, String delimiter, MutableComponent... comments) {
        return new OptionColor(i18n, defaultValue, delimiter, comments);
    }
}
