package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum ModeType implements ConfigOptionListEntry<ModeType> {
    MULTI(I18n.MODE_TYPE_MULTI),
    SINGLE(I18n.MODE_TYPE_SINGLE);

    private final I18n i18n;

    ModeType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
