package me.aleksilassila.litematica.printer.utils.mods;

import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialReservation;
import me.aleksilassila.litematica.printer.integration.inventory.PickBlockRequestPolicy;
import me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerInvocationPolicy;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Compatibility boundary for pick-block and the historical quick-shulker implementation.
 *
 * <p>User pick requests enter the shared material coordinator here. The class name is retained
 * because existing printer code also uses it for Quick Shulker lifecycle callbacks.</p>
 */
public final class QuickShulkerBridge {
    private QuickShulkerBridge() {
    }

    public static void requestItem(Item item) {
        if (item != null) {
            requestItem(item, MaterialRequest.Source.OTHER);
        }
    }

    public static MaterialReservation requestItem(Item item, MaterialRequest.Source source) {
        if (item == null) {
            return new MaterialReservation(0L, MaterialReservation.State.UNAVAILABLE);
        }
        return RuntimeAccess.get().materialRequests().request(item, source);
    }

    public static MaterialReservation requestItems(Item[] items, MaterialRequest.Source source) {
        if (items == null || items.length == 0) {
            return new MaterialReservation(0L, MaterialReservation.State.UNAVAILABLE);
        }
        return RuntimeAccess.get().materialRequests().request(items, source);
    }

    /** Submits one ordered provider request for vanilla or Litematica pick-block. */
    public static boolean handlePickBlock(LocalPlayer player, Item item) {
        return handleMissingPickBlock(player, item);
    }

    private static boolean handleMissingPickBlock(LocalPlayer player, Item item) {
        Minecraft client = Minecraft.getInstance();
        if (player == null || item == null || item == Items.AIR
                || client.gameMode == null
                || !QuickShulkerInvocationPolicy.allowsPickBlock(
                        player.getAbilities().instabuild,
                        player.isSpectator()
                )
                || player.containerMenu != player.inventoryMenu
                || InventoryUtils.playerHasItemInInventory(player, item)) {
            return false;
        }

        if (Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
            RuntimeAccess.get().quickShulkerAdapter().allowExternalRequest();
        }

        MaterialReservation reservation = requestItem(item, MaterialRequest.Source.PICK_BLOCK);
        var broker = RuntimeAccess.get().actionBroker();
        return PickBlockRequestPolicy.shouldConsume(reservation.state())
                || broker.isResourceHeld(ResourceLease.INVENTORY)
                || broker.isResourceHeld(ResourceLease.CONTAINER)
                || broker.isResourceHeld(ResourceLease.MAIN_HAND);
    }

    public static boolean handlePickBlock(LocalPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()
                || ItemStack.isSameItemSameComponents(player.getMainHandItem(), stack)) {
            return false;
        }
        return handleMissingPickBlock(player, stack.getItem());
    }

    public static boolean handleEasyPlacePickBlock(LocalPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()
                || ItemStack.isSameItemSameComponents(player.getMainHandItem(), stack)) {
            return false;
        }
        return handleMissingPickBlock(player, stack.getItem());
    }

    public static boolean switchItem() {
        return Configs.Placement.QUICK_SHULKER.getBooleanValue()
                && RuntimeAccess.get().quickShulkerAdapter().switchItem();
    }

    public static boolean hasPendingRequest() {
        return RuntimeAccess.get().quickShulkerAdapter().hasPendingRequest();
    }

    public static boolean isOpenHandler() {
        return RuntimeAccess.get().quickShulkerAdapter().isOpenHandler();
    }

    public static boolean shouldPause() {
        return RuntimeAccess.get().quickShulkerAdapter().shouldPause();
    }

    public static boolean shouldSuppressContainerScreen(int containerId) {
        return RuntimeAccess.get().quickShulkerAdapter().shouldSuppressContainerScreen(containerId);
    }

    public static void onContainerOpen(int containerId, net.minecraft.world.inventory.MenuType<?> type) {
        if (Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
            RuntimeAccess.get().quickShulkerAdapter().onContainerOpen(containerId, type);
        }
    }

    public static void onTick() {
        // An already-sent click can be acknowledged after either feature switch is turned off.
        ShulkerUtils.tickRecovery();
        RuntimeAccess.get().quickShulkerAdapter().tick();
    }

    public static void onSlotUpdate(int containerId, int slot, ItemStack stack) {
        ShulkerUtils.onSlotUpdate(containerId, slot, stack);
        if (me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.enabled()) {
            me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log(
                    "server-slot menu=" + containerId + " slot=" + slot + " stack="
                    + me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.stack(stack));
        }
        if (Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
            RuntimeAccess.get().quickShulkerAdapter().onSlotUpdate(containerId, slot, stack);
        }
    }

    public static void onInventoryContent(int containerId, java.util.List<ItemStack> items, ItemStack carried) {
        RuntimeAccess.get().quickShulkerAdapter().onInventoryContent(containerId, items, carried);
    }

    public static void onMainHandUse(LocalPlayer player) {
        if (Configs.Placement.QUICK_SHULKER.getBooleanValue()) {
            RuntimeAccess.get().quickShulkerAdapter().onMainHandUse(player);
        }
    }

    public static void resetRuntime() {
        RuntimeAccess.get().quickShulkerAdapter().reset();
    }
}
