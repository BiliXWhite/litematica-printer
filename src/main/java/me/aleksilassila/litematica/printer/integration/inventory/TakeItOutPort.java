package me.aleksilassila.litematica.printer.integration.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Compatibility boundary for Take It Out's asynchronous requests and mod-owned wait marker. */
public interface TakeItOutPort {
    ItemStack awaitingStack();

    default boolean isAwaitingItem(Item item) {
        ItemStack stack = awaitingStack();
        return item != null && !stack.isEmpty() && stack.is(item);
    }

    boolean tryRequestItem(Item item, boolean emptyShulkerOnly);

    void resetPending();
}
