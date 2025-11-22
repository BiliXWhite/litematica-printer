package me.aleksilassila.litematica.printer.bilixwhite.gui;

import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;

public enum ButtonType {
    PRINTER_SETTINGS("menu.settings_button");

    private final String labelKey;
    ButtonType(String labelKey)
    {
        this.labelKey = labelKey;
    }

    public String getLabelKey()
    {
        return this.labelKey;
    }

    public String getDisplayName()
    {
        return StringUtils.get(this.labelKey).getString();
    }
}