package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** Selects, reserves and requests material stacks without owning inventory transport state. */
public final class MaterialSelector {
    private static final int MAIN_INVENTORY_SLOT_COUNT = 36;

    private MaterialSelector() {
    }

    public static boolean switchToItems(LocalPlayer player, Item[] items, int reserveCount) {
        if (player == null || RuntimeAccess.get().inventorySwitchGuard().isWaiting()) {
            return false;
        }
        if (items == null || items.length == 0) {
            items = new Item[]{Items.AIR};
        }
        Inventory inventory = player.getInventory();
        ItemStack mainHandStack = player.getMainHandItem();
        for (Item item : items) {
            if (mainHandStack.getItem().equals(item)
                    && getConsumableSurplus(player, mainHandStack, null, reserveCount) > 0) {
                return true;
            }
        }
        Minecraft client = Minecraft.getInstance();
        if (PlayerUtils.getAbilities(player).instabuild) {
            return InventoryUtils.setPickedItemToHand(new ItemStack(items[0]), client);
        }
        for (Item item : items) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack itemStack = inventory.getItem(slot);
                if (itemStack.getItem().equals(item)
                        && getConsumableSurplus(player, itemStack, null, reserveCount) > 0) {
                    boolean needsInventoryConfirmation = !Inventory.isHotbarSlot(slot);
                    if (InventoryUtils.setPickedItemToHand(slot, itemStack, client)) {
                        return !needsInventoryConfirmation
                                || !RuntimeAccess.get().inventorySwitchGuard().markSwitchIfNeeded(item);
                    }
                    return false;
                }
            }
        }
        QuickShulkerBridge.requestItems(items, MaterialRequest.Source.PRINT);
        return false;
    }

    public static boolean switchToMatchingStack(
            LocalPlayer player,
            Predicate<ItemStack> predicate,
            ItemStack creativeFallback,
            int reserveCount
    ) {
        if (player == null || predicate == null || RuntimeAccess.get().inventorySwitchGuard().isWaiting()) {
            return false;
        }
        ItemStack mainHandStack = player.getMainHandItem();
        if (predicate.test(mainHandStack)
                && getConsumableSurplus(player, mainHandStack, predicate, reserveCount) > 0) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)
                    || getConsumableSurplus(player, stack, predicate, reserveCount) <= 0) {
                continue;
            }
            boolean needsInventoryConfirmation = !Inventory.isHotbarSlot(slot);
            if (InventoryUtils.setPickedItemToHand(slot, stack, client)) {
                return !needsInventoryConfirmation
                        || !RuntimeAccess.get().inventorySwitchGuard().markSwitchIfNeeded(stack.getItem());
            }
            return false;
        }

        if (!PlayerUtils.getAbilities(player).instabuild
                && MaterialRequest.isEmptyShulker(creativeFallback)
                && predicate.test(creativeFallback)) {
            RuntimeAccess.get().materialRequests().requestEmptyShulker(creativeFallback.getItem());
        }
        return PlayerUtils.getAbilities(player).instabuild
                && creativeFallback != null
                && predicate.test(creativeFallback)
                && InventoryUtils.setPickedItemToHand(creativeFallback.copy(), client);
    }

    public static int getConsumableSurplus(
            LocalPlayer player,
            ItemStack stack,
            @Nullable Predicate<ItemStack> requiredStackPredicate,
            int reserveCount
    ) {
        if (player == null || stack == null) {
            return 0;
        }
        if (reserveCount < 0
                || PlayerUtils.getAbilities(player).instabuild
                || stack.isEmpty()
                || stack.isDamageableItem()) {
            return Integer.MAX_VALUE;
        }
        Predicate<ItemStack> predicate = requiredStackPredicate != null
                ? requiredStackPredicate
                : candidate -> candidate.is(stack.getItem());
        return Math.max(0, countMatchingMainInventory(player, predicate) - reserveCount);
    }

    public static ItemStack findReserveBlockedStack(
            LocalPlayer player,
            Item[] items,
            @Nullable Predicate<ItemStack> requiredStackPredicate,
            int reserveCount
    ) {
        if (player == null || reserveCount < 0 || PlayerUtils.getAbilities(player).instabuild) {
            return ItemStack.EMPTY;
        }
        Inventory inventory = player.getInventory();
        int size = Math.min(MAIN_INVENTORY_SLOT_COUNT, inventory.getContainerSize());
        if (requiredStackPredicate != null) {
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()
                        && requiredStackPredicate.test(stack)
                        && getConsumableSurplus(player, stack, requiredStackPredicate, reserveCount) <= 0) {
                    return stack.copy();
                }
            }
            return ItemStack.EMPTY;
        }

        Item[] targetItems = items == null || items.length == 0 ? new Item[]{Items.AIR} : items;
        for (Item item : targetItems) {
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()
                        && stack.is(item)
                        && getConsumableSurplus(player, stack, null, reserveCount) <= 0) {
                    return stack.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static int countMatchingMainInventory(LocalPlayer player, Predicate<ItemStack> predicate) {
        Inventory inventory = player.getInventory();
        int size = Math.min(MAIN_INVENTORY_SLOT_COUNT, inventory.getContainerSize());
        int count = 0;
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
