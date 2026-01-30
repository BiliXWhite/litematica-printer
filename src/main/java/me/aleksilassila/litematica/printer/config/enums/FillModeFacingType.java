package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum FillModeFacingType implements ConfigOptionListEntry<FillModeFacingType> {
    DOWN("fillModeFacing.down"),
    UP("fillModeFacing.up"),
    WEST("fillModeFacing.west"),
    EAST("fillModeFacing.east"),
    NORTH("fillModeFacing.north"),
    SOUTH("fillModeFacing.south");

    private final I18n i18n;

    FillModeFacingType(String translateKey) {
        this.i18n = I18n.of(translateKey);
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
