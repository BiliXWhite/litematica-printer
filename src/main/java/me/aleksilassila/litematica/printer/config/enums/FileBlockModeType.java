package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum FileBlockModeType implements ConfigOptionListEntry<FileBlockModeType> {
    WHITELIST(I18n.FILE_BLOCK_MODE_TYPE_WHITELIST),
    HANDHELD(I18n.FILE_BLOCK_MODE_TYPE_HANDHELD);

    private final I18n i18n;

    FileBlockModeType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
