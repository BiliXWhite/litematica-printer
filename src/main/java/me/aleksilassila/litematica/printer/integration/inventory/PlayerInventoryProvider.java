package me.aleksilassila.litematica.printer.integration.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/** First provider in the chain; it never mutates inventory state. */
public final class PlayerInventoryProvider implements InventoryProvider {
    private final Minecraft client;

    public PlayerInventoryProvider(Minecraft client) {
        this.client = client;
    }

    @Override
    public String id() {
        return "player_inventory";
    }

    @Override
    public MaterialReservation request(MaterialRequest request) {
        LocalPlayer player = this.client.player;
        if (player == null || !player.containerMenu.equals(player.inventoryMenu)) {
            return unavailable(request);
        }
        Inventory inventory = player.getInventory();
        int size = Math.min(36, inventory.getContainerSize());
        for (Item acceptedItem : request.acceptedItems()) {
            int count = 0;
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.is(acceptedItem) && request.acceptsStack(stack)) {
                    count += stack.getCount();
                    if (count >= request.minimumCount()) {
                        return MaterialReservation.available(request, acceptedItem);
                    }
                }
            }
        }
        return unavailable(request);
    }

    @Override
    public MaterialReservation status(MaterialRequest request) {
        return this.request(request);
    }

    private static MaterialReservation unavailable(MaterialRequest request) {
        return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
    }
}
