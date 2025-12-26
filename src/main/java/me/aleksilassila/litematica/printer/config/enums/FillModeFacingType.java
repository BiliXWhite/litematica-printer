package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum FillModeFacingType implements ConfigOptionListEntry<FillModeFacingType> {
    DOWN(I18n.FILL_MODE_FACING_DOWN),
    UP(I18n.FILL_MODE_FACING_UP),
    WEST(I18n.FILL_MODE_FACING_WEST),
    EAST(I18n.FILL_MODE_FACING_EAST),
    NORTH(I18n.FILL_MODE_FACING_NORTH),
    SOUTH(I18n.FILL_MODE_FACING_SOUTH);

    private final I18n i18n;

    FillModeFacingType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
