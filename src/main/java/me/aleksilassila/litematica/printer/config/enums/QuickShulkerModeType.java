package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum QuickShulkerModeType implements ConfigOptionListEntry<QuickShulkerModeType> {
    CLICK_SLOT("printerQuickShulkerMode.click_slot"),
    INVOKE("printerQuickShulkerMode.invoke");

    private final I18n i18n;

    QuickShulkerModeType(String translateKey) {
        this.i18n = I18n.config(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
