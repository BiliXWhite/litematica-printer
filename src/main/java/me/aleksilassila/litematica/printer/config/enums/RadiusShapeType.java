package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum RadiusShapeType implements ConfigOptionListEntry<RadiusShapeType> {
    SPHERE(I18n.ITERATOR_SHAPE_TYPE_SPHERE),
    OCTAHEDRON(I18n.ITERATOR_SHAPE_TYPE_OCTAHEDRON),
    CUBE(I18n.ITERATOR_SHAPE_TYPE_CUBE);

    private final I18n i18n;

    RadiusShapeType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
