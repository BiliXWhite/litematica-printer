package me.aleksilassila.litematica.printer.bilixwhite.utils;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class PreprocessUtils {
    public static int getSelectedSlot(PlayerInventory inventory) {
        //#if MC > 12104
        return inventory.getSelectedSlot();
        //#else
        //$$ return inventory.selectedSlot;
        //#endif
    }

    public static void setSelectedSlot(PlayerInventory inventory, int slot) {
        //#if MC > 12101
        inventory.setSelectedSlot(slot);
        //#else
        //$$ inventory.selectedSlot = slot;
        //#endif
    }

    public static DefaultedList<ItemStack> getMainStacks(PlayerInventory inventory) {
        //#if MC > 12104
        return inventory.getMainStacks();
        //#else
        //$$ return inventory.main;
        //#endif
    }
}
