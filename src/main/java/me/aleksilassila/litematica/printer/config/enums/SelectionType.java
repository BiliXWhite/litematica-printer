package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum SelectionType implements ConfigOptionListEntry<SelectionType> {
    LITEMATICA_SELECTION("selectionType.litematica.selection"),
    LITEMATICA_RENDER_LAYER("selectionType.litematica.renderLayer"),
    LITEMATICA_SELECTION_BELOW_PLAYER("selectionType.litematica.selection.belowPlayer"),
    LITEMATICA_SELECTION_ABOVE_PLAYER("selectionType.litematica.selection.abovePlayer");

    private final I18n i18n;

    SelectionType(String translateKey) {
        this.i18n = I18n.of(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
