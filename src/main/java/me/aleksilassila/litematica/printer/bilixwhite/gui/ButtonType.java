package me.aleksilassila.litematica.printer.bilixwhite.gui;

import me.aleksilassila.litematica.printer.I18n;

public enum ButtonType {
    PRINTER_SETTINGS(I18n.MENU_SETTINGS_BUTTON);

    private final I18n i18n;

    ButtonType(I18n i18n) {
        this.i18n = i18n;
    }

    public String getLabelKey() {
        return this.i18n.getKey();
    }

    public String getDisplayName() {
        return this.i18n.getKeyComponent().getString();
    }
}