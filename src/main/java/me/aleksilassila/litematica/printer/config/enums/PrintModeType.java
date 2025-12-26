package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum PrintModeType implements ConfigOptionListEntry<PrintModeType> {
    PRINTER(I18n.PRINT_MODE_PRINTER),
    MINE(I18n.PRINT_MODE_MINE),
    FLUID(I18n.PRINT_MODE_FLUID),
    FILL(I18n.PRINT_MODE_FILL),
    //        REPLACE(I18n.PRINT_MODE_REPLACE),
    BEDROCK(I18n.PRINT_MODE_BEDROCK);

    private final I18n i18n;

    PrintModeType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
