package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum ExcavateListMode implements ConfigOptionListEntry<ExcavateListMode> {
    TWEAKEROO(I18n.EXCAVATE_LIST_MODE_TWEAKEROO),
    CUSTOM(I18n.EXCAVATE_LIST_MODE_CUSTOM);

    private final I18n i18n;

    ExcavateListMode(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
