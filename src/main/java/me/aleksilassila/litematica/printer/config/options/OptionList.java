package me.aleksilassila.litematica.printer.config.options;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.Component;

public class OptionList extends ConfigOptionList {
    private OptionList(I18n i18n, IConfigOptionListEntry defaultValue) {
        super(i18n.getConfigNameString(), defaultValue, i18n.getConfigCommentString());
    }

    private OptionList(I18n i18n, IConfigOptionListEntry defaultValue, String delimiter, Component... comments) {
        super(i18n.getConfigNameString(), defaultValue, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionList create(I18n i18n, IConfigOptionListEntry defaultValue) {
        return new OptionList(i18n, defaultValue);
    }

    public static OptionList create(I18n i18n, IConfigOptionListEntry defaultValue, String delimiter, Component... comments) {
        return new OptionList(i18n, defaultValue, delimiter, comments);
    }

    public static OptionList create(I18n i18n, IConfigOptionListEntry defaultValue, Component... comments) {
        return new OptionList(i18n, defaultValue, "", comments);
    }
}
