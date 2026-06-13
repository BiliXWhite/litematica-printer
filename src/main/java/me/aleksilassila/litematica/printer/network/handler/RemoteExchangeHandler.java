//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.handler;

import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.container.ContainerItemResolver;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import me.aleksilassila.litematica.printer.network.payload.RemoteExchangePayload;
import me.aleksilassila.litematica.printer.network.payload.RemoteExchangeResultPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RemoteExchangeHandler {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
            RemoteExchangePayload.TYPE,
            (payload, context) -> {
                //#if MC >= 12100
                MinecraftServer server = context.server();
                //#else
                //$$ MinecraftServer server = context.player().getServer();
                //#endif
                handle(server, context.player(), payload);
            }
        );
    }

    private static void handle(MinecraftServer server, ServerPlayer player,
                                RemoteExchangePayload payload) {
        server.execute(() -> {
            try {
                int returned = returnItems(player, payload);
                ContainerItemResolver.ResolveResult taken = takeItems(player, payload);

                ServerPlayNetworking.send(player,
                    new RemoteExchangeResultPayload(
                        payload.getTakePos(),
                        taken != null ? taken.type() : RemoteResultType.SUCCESS,
                        taken != null ? taken.extractedCount() : 0,
                        returned
                    ));
            } catch (Exception e) {
                Reference.LOGGER.error("Exchange error for {}: {}",
                        player.getName().getString(), e.getMessage(), e);
                ServerPlayNetworking.send(player,
                    new RemoteExchangeResultPayload(
                        payload.getTakePos(), RemoteResultType.INTERNAL_ERROR, 0, 0));
            }
        });
    }

    private static int returnItems(ServerPlayer player, RemoteExchangePayload payload) {
        String id = payload.getReturnItemId();
        int count = payload.getReturnCount();
        if (count <= 0 || id.isEmpty()) return 0;

        BlockPos pos = payload.getReturnPos();
        //#if MC >= 12000
        net.minecraft.server.level.ServerLevel level = player.level();
        //#else
        //$$ net.minecraft.server.level.ServerLevel level = player.getLevel();
        //#endif
        if (!level.isLoaded(pos)) return 0;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container container)) return 0;

        Item item = resolveItem(id);
        if (item == null) return 0;

        Inventory inv = player.getInventory();
        int maxInsertable = simulateInsert(container, inv, item);
        int toMove = Math.min(count, maxInsertable);
        if (toMove <= 0) return 0;

        int moved = 0;
        for (int i = 0; i < 36 && moved < toMove; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) continue;

            int take = Math.min(toMove - moved, stack.getCount());
            ItemStack taken = stack.split(take);
            int before = taken.getCount();
            int inserted = insertIntoContainer(container, taken);
            moved += inserted;

            if (inserted < before) {
                inv.add(taken);
                if (!taken.isEmpty()) player.drop(taken, false);
                break;
            }
        }

        container.setChanged();
        return moved;
    }

    private static ContainerItemResolver.ResolveResult takeItems(ServerPlayer player,
                                                                  RemoteExchangePayload payload) {
        String id = payload.getTakeItemId();
        int slot = payload.getTakeSlot();
        if (id.isEmpty() || slot < 0) return null;

        return ContainerItemResolver.resolveItem(player, payload.getTakePos(), id, slot);
    }

    private static Item resolveItem(String itemId) {
        //#if MC >= 11903
        return BuiltInRegistries.ITEM.getOptional(
        //#else
        //$$ return net.minecraft.core.Registry.ITEM.getOptional(
        //#endif
            //#if MC >= 12105
            net.minecraft.resources.Identifier.parse(itemId)
            //#elseif MC >= 12101
            //$$ net.minecraft.resources.ResourceLocation.parse(itemId)
            //#else
            //$$ new net.minecraft.resources.ResourceLocation(itemId)
            //#endif
        ).orElse(null);
    }

    private static int simulateInsert(Container container, Inventory inv, Item item) {
        int size = container.getContainerSize();
        java.util.ArrayList<ItemStack> sim = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) sim.add(container.getItem(i).copy());

        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack playerStack = inv.getItem(i);
            if (playerStack.isEmpty() || !playerStack.is(item)) continue;

            int remaining = playerStack.getCount();
            for (int j = 0; j < size && remaining > 0; j++) {
                ItemStack slot = sim.get(j);
                if (slot.isEmpty()) {
                    int canFit = Math.min(remaining, playerStack.getMaxStackSize());
                    sim.set(j, playerStack.copyWithCount(canFit));
                    remaining -= canFit;
                } else if (isSameItem(slot, playerStack)) {
                    int space = slot.getMaxStackSize() - slot.getCount();
                    int canFit = Math.min(remaining, space);
                    slot.grow(canFit);
                    sim.set(j, slot);
                    remaining -= canFit;
                }
            }
            total += playerStack.getCount() - remaining;
        }
        return total;
    }

    private static int insertIntoContainer(Container container, ItemStack stack) {
        int original = stack.getCount();
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                int toPut = Math.min(stack.getMaxStackSize(), stack.getCount());
                container.setItem(i, stack.copyWithCount(toPut));
                stack.shrink(toPut);
            } else if (isSameItem(slot, stack)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    slot.grow(toAdd);
                    stack.shrink(toAdd);
                    container.setItem(i, slot);
                }
            }
        }
        return original - stack.getCount();
    }

    private static boolean isSameItem(ItemStack a, ItemStack b) {
        //#if MC >= 12005
        return ItemStack.isSameItemSameComponents(a, b);
        //#else
        //$$ return ItemStack.isSameItem(a, b);
        //#endif
    }
}
//#endif
