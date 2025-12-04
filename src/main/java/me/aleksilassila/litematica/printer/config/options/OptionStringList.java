package me.aleksilassila.litematica.printer.config.options;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.MutableComponent;

public class OptionStringList extends ConfigStringList {
    private OptionStringList(I18n i18n, ImmutableList<String> defaultValue) {
        super(i18n.getConfigNameString(), defaultValue, i18n.getConfigCommentString());
    }

    private OptionStringList(I18n i18n, ImmutableList<String> defaultValue, String delimiter, MutableComponent... comments) {
        super(i18n.getConfigNameString(), defaultValue, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionStringList create(I18n i18n, ImmutableList<String> defaultValue) {
        return new OptionStringList(i18n, defaultValue);
    }

    public static OptionStringList create(I18n i18n, ImmutableList<String> defaultValue, String delimiter, MutableComponent... comments) {
        return new OptionStringList(i18n, defaultValue, delimiter, comments);
    }

    public static OptionStringList create(I18n i18n, ImmutableList<String> defaultValue, MutableComponent... comments) {
        return new OptionStringList(i18n, defaultValue, "", comments);
    }
}
