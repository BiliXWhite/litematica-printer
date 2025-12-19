package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerInventoryUtils {

    public static NonNullList<ItemStack> getNonEquipmentItems(Player player) {
        //#if MC > 12104
        return player.getInventory().getNonEquipmentItems();
        //#else
        //$$ return player.getInventory().items;
        //#endif
    }

    public static int getSelectedSlot(Player player) {
        //#if MC > 12104
        return player.getInventory().getSelectedSlot();
        //#else
        //$$ return player.getInventory().selected;
        //#endif
    }

    public static void setSelectedSlot(Player player,int slot) {
        //#if MC > 12104
        player.getInventory().setSelectedSlot(slot);
        //#else
        //$$ player.getInventory().selected = slot;
        //#endif
    }
}