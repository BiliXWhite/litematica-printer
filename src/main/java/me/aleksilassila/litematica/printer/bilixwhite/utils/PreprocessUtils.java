package me.aleksilassila.litematica.printer.bilixwhite.utils;

import net.minecraft.entity.player.PlayerInventory;

public class PreprocessUtils {
    public static int getSelectedSlot(PlayerInventory inventory) {
        //#if MC > 12104
        return inventory.getSelectedSlot();
        //#else
        //$$ return inventory.selectedSlot;
        //#endif
    }
}
