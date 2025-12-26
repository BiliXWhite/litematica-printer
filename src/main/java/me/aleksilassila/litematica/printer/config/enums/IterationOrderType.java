package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum IterationOrderType implements ConfigOptionListEntry<IterationOrderType> {
    XYZ(I18n.ITERATION_ORDER_XYZ),
    XZY(I18n.ITERATION_ORDER_XZY),
    YXZ(I18n.ITERATION_ORDER_YXZ),
    YZX(I18n.ITERATION_ORDER_YZX),
    ZXY(I18n.ITERATION_ORDER_ZXY),
    ZYX(I18n.ITERATION_ORDER_ZYX);

    private final I18n i18n;

    IterationOrderType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
