package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum ExcavateListMode implements ConfigOptionListEntry<ExcavateListMode> {
    TWEAKEROO("excavateListMode.tweakeroo"),
    CUSTOM("excavateListMode.custom");

    private final I18n i18n;

    ExcavateListMode(String translateKey) {
        this.i18n = I18n.config(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
