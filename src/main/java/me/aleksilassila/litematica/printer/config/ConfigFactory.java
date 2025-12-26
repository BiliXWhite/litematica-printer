package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.StringUtils;
import me.aleksilassila.litematica.printer.I18n;

public class ConfigFactory {

    // 使用Mixin新增接口, 修改masa底层方法实现的
    public static <T extends ConfigBase<?>> T buildI18n(I18n i18n, T config) {
        ConfigBaseExtension extension = (ConfigBaseExtension) config;
        extension.litematica_printer$setTranslateNameKey(i18n.getConfigNameKey());
        extension.litematica_printer$setTranslateCommentKey(i18n.getConfigCommentKey());
        return config;
    }

    public static ConfigBooleanHotkeyed booleanHotkey(I18n i18n, boolean defaultValue, String defaultHotkey, KeybindSettings settings, String comment) {
        ConfigBooleanHotkeyed config = new ConfigBooleanHotkeyed(i18n.getId(), defaultValue, defaultHotkey, settings, comment, StringUtils.splitCamelCase(i18n.getId()));
        return buildI18n(i18n, config);
    }

    public static ConfigBooleanHotkeyed booleanHotkey(I18n i18n, boolean defaultValue, String defaultHotkey, KeybindSettings settings) {
        return booleanHotkey(i18n, defaultValue, defaultHotkey, settings, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigBooleanHotkeyed booleanHotkey(I18n i18n, boolean defaultValue, String defaultHotkey, String comment) {
        return booleanHotkey(i18n, defaultValue, defaultHotkey, KeybindSettings.DEFAULT, comment);
    }

    public static ConfigBooleanHotkeyed booleanHotkey(I18n i18n, boolean defaultValue, String defaultHotkey) {
        return booleanHotkey(i18n, defaultValue, defaultHotkey, KeybindSettings.DEFAULT);
    }

    public static ConfigBooleanHotkeyed booleanHotkey(I18n i18n, boolean defaultValue) {
        return booleanHotkey(i18n, defaultValue, "");
    }

    public static ConfigBooleanHotkeyed booleanHotkey(I18n i18n) {
        return booleanHotkey(i18n, false);
    }

    public static ConfigBoolean bool(I18n i18n, boolean defaultValue, String comment) {
        ConfigBoolean config = new ConfigBoolean(i18n.getId(), defaultValue, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigBoolean bool(I18n i18n, boolean defaultValue) {
        return bool(i18n, defaultValue, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigBoolean bool(I18n i18n) {
        return bool(i18n, false);
    }


    public static ConfigHotkey hotkey(I18n i18n, String defaultStorageString, KeybindSettings settings, String comment) {
        ConfigHotkey config = new ConfigHotkey(i18n.getId(), defaultStorageString, settings, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigHotkey hotkey(I18n i18n, String defaultStorageString, KeybindSettings settings) {
        return hotkey(i18n, defaultStorageString, settings, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigHotkey hotkey(I18n i18n, String defaultStorageString) {
        return hotkey(i18n, defaultStorageString, KeybindSettings.DEFAULT);
    }

    public static ConfigHotkey hotkey(I18n i18n, KeybindSettings settings) {
        return hotkey(i18n, "", settings);
    }

    public static ConfigHotkey hotkey(I18n i18n) {
        return hotkey(i18n, "", KeybindSettings.DEFAULT);
    }


    public static ConfigInteger integer(I18n i18n, int defaultValue, int minValue, int maxValue, boolean useSlider, String comment) {
        ConfigInteger config = new ConfigInteger(i18n.getId(), defaultValue, minValue, maxValue, useSlider, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigInteger integer(I18n i18n, int defaultValue, int minValue, int maxValue, boolean useSlider) {
        return integer(i18n, defaultValue, minValue, maxValue, useSlider, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigInteger integer(I18n i18n, int defaultValue, int minValue, int maxValue) {
        return integer(i18n, defaultValue, minValue, maxValue, false);
    }

    public static ConfigInteger integer(I18n i18n, int defaultValue) {
        return integer(i18n, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static ConfigInteger integer(I18n i18n) {
        return integer(i18n, 0);
    }


    public static ConfigStringList stringList(I18n i18n, ImmutableList<String> defaultValue, String comment) {
        ConfigStringList config = new ConfigStringList(i18n.getId(), defaultValue, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigStringList stringList(I18n i18n, ImmutableList<String> defaultValue) {
        return stringList(i18n, defaultValue, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigString string(I18n i18n, String defaultValue, String comment) {
        ConfigString config = new ConfigString(i18n.getId(), defaultValue, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigString string(I18n i18n, String defaultValue) {
        return string(i18n, defaultValue, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigString string(I18n i18n) {
        return string(i18n, "");
    }


    public static ConfigOptionList optionList(I18n i18n, IConfigOptionListEntry defaultValue, String comment) {
        ConfigOptionList config = new ConfigOptionList(i18n.getId(), defaultValue, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigOptionList optionList(I18n i18n, IConfigOptionListEntry defaultValue) {
        return optionList(i18n, defaultValue, i18n.getConfigCommentComponent().getString());
    }

    public static ConfigColor color(I18n i18n, String defaultValue, String comment) {
        ConfigColor config = new ConfigColor(i18n.getId(), defaultValue, comment);
        return buildI18n(i18n, config);
    }

    public static ConfigColor color(I18n i18n, String defaultValue) {
        return color(i18n, defaultValue, i18n.getConfigCommentComponent().getString());
    }
}

