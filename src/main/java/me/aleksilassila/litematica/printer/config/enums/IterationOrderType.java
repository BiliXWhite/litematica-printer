package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

import me.aleksilassila.litematica.printer.iterator.IteratorAxis;

public enum IterationOrderType implements ConfigOptionListEntry<IterationOrderType> {

    XYZ(I18n.ITERATION_ORDER_XYZ, IteratorAxis.X, IteratorAxis.Y, IteratorAxis.Z),
    XZY(I18n.ITERATION_ORDER_XZY, IteratorAxis.X, IteratorAxis.Z, IteratorAxis.Y),
    YXZ(I18n.ITERATION_ORDER_YXZ, IteratorAxis.Y, IteratorAxis.X, IteratorAxis.Z),
    YZX(I18n.ITERATION_ORDER_YZX, IteratorAxis.Y, IteratorAxis.Z, IteratorAxis.X),
    ZXY(I18n.ITERATION_ORDER_ZXY, IteratorAxis.Z, IteratorAxis.X, IteratorAxis.Y),
    ZYX(I18n.ITERATION_ORDER_ZYX, IteratorAxis.Z, IteratorAxis.Y, IteratorAxis.X);

    private final I18n i18n;
    public final IteratorAxis[] axis;

    IterationOrderType(I18n i18n, IteratorAxis a1, IteratorAxis a2, IteratorAxis a3) {
        this.i18n = i18n;
        this.axis = new IteratorAxis[]{a1, a2, a3};
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
