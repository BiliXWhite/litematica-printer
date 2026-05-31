package me.aleksilassila.litematica.printer.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum ShulkerSource implements ConfigOptionListEntry<ShulkerSource> {
    MOD("shulkerSource.mod"),
    PLUGIN("shulkerSource.plugin");

    private final I18n i18n;

    ShulkerSource(String translateKey) {
        this.i18n = I18n.of(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
