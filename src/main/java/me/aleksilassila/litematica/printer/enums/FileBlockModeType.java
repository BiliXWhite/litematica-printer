package me.aleksilassila.litematica.printer.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum FileBlockModeType implements ConfigOptionListEntry<FileBlockModeType> {
    WHITELIST("fillBlockModeType.whitelist"),
    HANDHELD("fillBlockModeType.handheld");

    private final I18n i18n;

    FileBlockModeType(String translateKey) {
        this.i18n = I18n.of(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
