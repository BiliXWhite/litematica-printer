package me.aleksilassila.litematica.printer.config;

import me.aleksilassila.litematica.printer.config.builder.*;

public class ConfigBuilders {
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
