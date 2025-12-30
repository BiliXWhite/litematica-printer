package me.aleksilassila.litematica.printer.bilixwhite.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public class PreprocessUtils {
    public static Component getNameFromItem(Item item) {
        //#if MC > 12101
        return item.getName();
        //#else
        //$$ return item.getDescription();
        //#endif
    }
}
