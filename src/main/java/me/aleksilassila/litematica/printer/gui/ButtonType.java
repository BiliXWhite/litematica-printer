package me.aleksilassila.litematica.printer.gui;

import me.aleksilassila.litematica.printer.I18n;

public enum ButtonType {
    PRINTER_SETTINGS(I18n.of("menu.settings_button"));

    private final I18n i18n;

    ButtonType(I18n i18n) {
        this.i18n = i18n;
    }

    public String getDisplayName() {
        return this.i18n.getName().getString();
    }
}