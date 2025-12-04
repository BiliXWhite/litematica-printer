package me.aleksilassila.litematica.printer.config.options;

import fi.dy.masa.malilib.config.options.ConfigInteger;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.network.chat.MutableComponent;

public class OptionInteger extends ConfigInteger {
    private OptionInteger(I18n i18n, int defaultValue) {
        super(i18n.getConfigNameString(), defaultValue, i18n.getConfigCommentString());
    }

    private OptionInteger(I18n i18n, int defaultValue, int minValue, int maxValue) {
        super(i18n.getConfigNameString(), defaultValue, minValue, maxValue, i18n.getConfigCommentString());
    }

    private OptionInteger(I18n i18n, int defaultValue, int minValue, int maxValue, String delimiter, MutableComponent... comments) {
        super(i18n.getConfigNameString(), defaultValue, minValue, maxValue, StringUtils.mergeComments(delimiter, comments));
    }

    public static OptionInteger create(I18n i18n, int defaultValue) {
        return new OptionInteger(i18n, defaultValue);
    }

    public static OptionInteger create(I18n i18n, int defaultValue, int minValue, int maxValue) {
        return new OptionInteger(i18n, defaultValue, minValue, maxValue);
    }

    public static OptionInteger createMin(I18n i18n, int defaultValue, int minValue) {
        return new OptionInteger(i18n, defaultValue, minValue, Integer.MAX_VALUE);
    }

    public static OptionInteger createMax(I18n i18n, int defaultValue, int maxValue) {
        return new OptionInteger(i18n, defaultValue, Integer.MIN_VALUE, maxValue);
    }

    public static OptionInteger create(I18n i18n, int defaultValue, String delimiter, MutableComponent... comments) {
        return new OptionInteger(i18n, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE, delimiter, comments);
    }

    public static OptionInteger create(I18n i18n, int defaultValue, MutableComponent... comments) {
        return new OptionInteger(i18n, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE, "", comments);
    }
}
