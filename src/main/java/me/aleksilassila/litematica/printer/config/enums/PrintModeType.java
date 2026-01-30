package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum PrintModeType implements ConfigOptionListEntry<PrintModeType> {
    PRINTER("printMode.printer"),
    MINE("printMode.mine"),
    FLUID("printMode.fluid"),
    FILL("printMode.fill"),
    // REPLACE("printMode.replace"),
    BEDROCK("printMode.bedrock");

    private final I18n i18n;

    PrintModeType(String translateKey) {
        this.i18n = I18n.of(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
