package me.aleksilassila.litematica.printer.config;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.builder.*;

public class ConfigBuilders {
//    public static BooleanHotkeyConfigBuilder booleanHotkey(I18n i18n) {
//        return new BooleanHotkeyConfigBuilder(i18n);
//    }
//
//    public static BooleanConfigBuilder bool(I18n i18n) {
//        return new BooleanConfigBuilder(i18n);
//    }
//
//    public static HotkeyConfigBuilder hotkey(I18n i18n) {
//        return new HotkeyConfigBuilder(i18n);
//    }
//
//    public static IntegerConfigBuilder integer(I18n i18n) {
//        return new IntegerConfigBuilder(i18n);
//    }
//
//    public static StringListConfigBuilder stringList(I18n i18n) {
//        return new StringListConfigBuilder(i18n);
//    }
//
//    public static StringConfigBuilder string(I18n i18n) {
//        return new StringConfigBuilder(i18n);
//    }
//
//    public static OptionListConfigBuilder optionList(I18n i18n) {
//        return new OptionListConfigBuilder(i18n);
//    }
//
//    public static ColorConfigBuilder color(I18n i18n) {
//        return new ColorConfigBuilder(i18n);
//    }    

    public static BooleanHotkeyConfigBuilder booleanHotkey(String translateKey) {
        return new BooleanHotkeyConfigBuilder(translateKey);
    }

    public static BooleanConfigBuilder bool(String translateKey) {
        return new BooleanConfigBuilder(translateKey);
    }

    public static HotkeyConfigBuilder hotkey(String translateKey) {
        return new HotkeyConfigBuilder(translateKey);
    }

    public static IntegerConfigBuilder integer(String translateKey) {
        return new IntegerConfigBuilder(translateKey);
    }

    public static StringListConfigBuilder stringList(String translateKey) {
        return new StringListConfigBuilder(translateKey);
    }

    public static StringConfigBuilder string(String translateKey) {
        return new StringConfigBuilder(translateKey);
    }

    public static OptionListConfigBuilder optionList(String translateKey) {
        return new OptionListConfigBuilder(translateKey);
    }

    public static ColorConfigBuilder color(String translateKey) {
        return new ColorConfigBuilder(translateKey);
    }
}
