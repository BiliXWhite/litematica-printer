package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

import static me.aleksilassila.litematica.printer.printer.MyBox.Axis;

public enum IterationOrderType implements ConfigOptionListEntry<IterationOrderType> {

    XYZ(I18n.ITERATION_ORDER_XYZ, Axis.X, Axis.Y, Axis.Z),
    XZY(I18n.ITERATION_ORDER_XZY, Axis.X, Axis.Z, Axis.Y),
    YXZ(I18n.ITERATION_ORDER_YXZ, Axis.Y, Axis.X, Axis.Z),
    YZX(I18n.ITERATION_ORDER_YZX, Axis.Y, Axis.Z, Axis.X),
    ZXY(I18n.ITERATION_ORDER_ZXY, Axis.Z, Axis.X, Axis.Y),
    ZYX(I18n.ITERATION_ORDER_ZYX, Axis.Z, Axis.Y, Axis.X);

    private final I18n i18n;
    public final Axis[] axis;

    IterationOrderType(I18n i18n, Axis a1, Axis a2, Axis a3) {
        this.i18n = i18n;
        this.axis = new Axis[]{a1, a2, a3};
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
