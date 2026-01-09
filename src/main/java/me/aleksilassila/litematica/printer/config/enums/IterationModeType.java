package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum IterationModeType implements ConfigOptionListEntry<IterationOrderType> {
    LINEAR(I18n.of("LINEAR")),   // 线性迭代
    CIRCLE(I18n.of("CIRCLE"));    // 圆环迭代

    private final I18n i18n;

    IterationModeType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }

}
