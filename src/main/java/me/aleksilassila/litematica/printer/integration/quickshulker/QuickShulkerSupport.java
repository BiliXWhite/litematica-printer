package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class QuickShulkerSupport {
    private static final Minecraft client = Minecraft.getInstance();

    public static boolean isSafeHotbarTarget(int hotbarSlot, int sourceMenuSlot, ItemStack stack) {
        return hotbarSlot >= 0 && hotbarSlot < 9 && hotbarSlot + 36 != sourceMenuSlot
                && !(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock);
    }

    public static boolean canRecoverCursor(ItemStack opened, ItemStack carried, ItemStack originalSlot) {
        return !opened.isEmpty() && originalSlot.isEmpty() && ItemStack.matches(opened, carried);
    }

    public static boolean acceptsMenu(net.minecraft.world.inventory.MenuType<?> type, boolean clickMode) {
        return type == net.minecraft.world.inventory.MenuType.SHULKER_BOX
                || clickMode && type == net.minecraft.world.inventory.MenuType.GENERIC_9x3;
    }

    public static int switchPlayerInvToHotbarAir(int slot) {
        if (client.player == null) return -1;
        LocalPlayer player = client.player;
        AbstractContainerMenu sc = player.containerMenu;
        NonNullList<Slot> slots = sc.slots;
        int i = sc.equals(player.inventoryMenu) ? 9 : 0;
        int playerSlotOrdinal = 0;
        for (; i < slots.size(); i++) {
            if (!(slots.get(i).container instanceof Inventory)) {
                continue;
            }
            if (slots.get(i).getItem().isEmpty()) {
                fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, i, slot);
                return playerSlotOrdinal < 27 ? playerSlotOrdinal + 9 : playerSlotOrdinal - 27;
            }
            playerSlotOrdinal++;
        }
        return -1;
    }

}
