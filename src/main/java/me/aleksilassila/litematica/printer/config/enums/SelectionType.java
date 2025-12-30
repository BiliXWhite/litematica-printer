package me.aleksilassila.litematica.printer.config.enums;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.ConfigOptionListEntry;

public enum SelectionType implements ConfigOptionListEntry<SelectionType> {
    LITEMATICA_SELECTION(I18n.SELECTION_TYPE_LITEMATICA_SELECTION), // 投影选区
    LITEMATICA_RENDER_LAYER(I18n.SELECTION_TYPE_LITEMATICA_RENDER_LAYER), // 投影渲染层
    LITEMATICA_SELECTION_BELOW_PLAYER(I18n.SELECTION_TYPE_LITEMATICA_SELECTION_BELOW_PLAYER), // 投影选区 玩家之下
    LITEMATICA_SELECTION_ABOVE_PLAYER(I18n.SELECTION_TYPE_LITEMATICA_SELECTION_ABOVE_PLAYER); // 投影选区 玩家之上

    private final I18n i18n;

    SelectionType(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }
}
