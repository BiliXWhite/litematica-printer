package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum QuickShulkerModeType implements ConfigOptionListEntry<QuickShulkerModeType> {
    CLICK_SLOT(I18n.PRINTER_QUICK_SHULKER_MODE_CLICK_SLOT),
    INVOKE(I18n.PRINTER_QUICK_SHULKER_MODE_INVOKE);

    private final I18n i18n;

    QuickShulkerModeType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
