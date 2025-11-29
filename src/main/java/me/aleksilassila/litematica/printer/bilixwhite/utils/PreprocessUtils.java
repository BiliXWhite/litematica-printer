package me.aleksilassila.litematica.printer.bilixwhite.utils;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

//#if MC > 11802
import net.minecraft.core.registries.BuiltInRegistries;
//#else
//$$import net.minecraft.core.Registry;
//#endif

public class PreprocessUtils {
    public static int getSelectedSlot(Inventory inventory) {
        //#if MC > 12104
        return inventory.getSelectedSlot();
        //#else
        //$$ return inventory.selected;
        //#endif
    }

    public static void setSelectedSlot(Inventory inventory, int slot) {
        //#if MC > 12101
        inventory.setSelectedSlot(slot);
        //#else
        //$$ inventory.selected = slot;
        //#endif
    }

    public static NonNullList<ItemStack> getMainStacks(Inventory inventory) {
        //#if MC > 12104
        return inventory.getNonEquipmentItems();
        //#else
        //$$ return inventory.items;
        //#endif
    }

    public static Vec3i getVec3iFromDirection(Direction direction) {
        //#if MC > 12101
        return direction.getUnitVec3i();
        //#else
        //$$ return direction.getNormal();
        //#endif
    }

    public static Component getNameFromItem(Item item) {
        //#if MC > 12101
        return item.getName();
        //#else
        //$$ return item.getDescription();
        //#endif
    }

    public static
    //#if MC > 11802
    BuiltInRegistries
    //#else
    //$$ Registry
    //#endif
    getRegistries() {
        try {
            return
                    //#if MC > 11802
                    BuiltInRegistries
                    //#else
                    //$$ Registry
                    //#endif
                    .class.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of BuiltInRegistries", e);
        }
    }
}
