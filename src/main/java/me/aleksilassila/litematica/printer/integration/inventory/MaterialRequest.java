package me.aleksilassila.litematica.printer.integration.inventory;

import net.minecraft.world.item.Item;

import java.util.LinkedHashSet;
import java.util.List;

/** Immutable, tokenized request for one logical material requirement. */
public record MaterialRequest(
        long token,
        List<Item> acceptedItems,
        Item preferredItem,
        int minimumCount,
        Source source,
        boolean emptyShulkerOnly
) {
    public MaterialRequest(long token, List<Item> acceptedItems, Item preferredItem, int minimumCount, Source source) {
        this(token, acceptedItems, preferredItem, minimumCount, source, false);
    }
    public MaterialRequest {
        if (token <= 0L) throw new IllegalArgumentException("token must be positive");
        if (acceptedItems == null || acceptedItems.isEmpty()) {
            throw new IllegalArgumentException("acceptedItems must not be empty");
        }
        LinkedHashSet<Item> normalized = new LinkedHashSet<>();
        for (Item item : acceptedItems) {
            if (item == null) throw new IllegalArgumentException("acceptedItems must not contain null");
            normalized.add(item);
        }
        acceptedItems = List.copyOf(normalized);
        if (preferredItem == null || !acceptedItems.contains(preferredItem)) {
            throw new IllegalArgumentException("preferredItem must be accepted");
        }
        if (minimumCount <= 0) throw new IllegalArgumentException("minimumCount must be positive");
        if (source == null) throw new IllegalArgumentException("source must not be null");
    }

    public static MaterialRequest single(long token, Item item, int minimumCount, Source source) {
        return new MaterialRequest(token, List.of(item), item, minimumCount, source);
    }

    public boolean accepts(Item item) {
        return item != null && this.acceptedItems.contains(item);
    }

    public boolean acceptsStack(net.minecraft.world.item.ItemStack stack) {
        return stack != null && !stack.isEmpty() && accepts(stack.getItem())
                && (!this.emptyShulkerOnly || isEmptyShulker(stack));
    }

    public static boolean isEmptyShulker(net.minecraft.world.item.ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock
                && fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1).stream()
                        .allMatch(net.minecraft.world.item.ItemStack::isEmpty);
    }

    public enum Source {
        PRINT,
        PICK_BLOCK,
        CHEST_TRACKER_SCREEN,
        OTHER
    }
}
