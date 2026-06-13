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

import java.util.ArrayList;
import java.util.List;

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
                // 先快照 → 还物 → 取物 → 再快照 → 算 delta，一起发回客户端修正本地背包
                byte[] before = snapshotInventory(player);
                int returned = returnItems(player, payload);
                ContainerItemResolver.ResolveResult taken = takeItems(player, payload);
                byte[] after = snapshotInventory(player);
                List<RemoteExchangeResultPayload.SlotSnapshot> delta = computeDelta(player, before, after);

                ServerPlayNetworking.send(player,
                    new RemoteExchangeResultPayload(
                        payload.getTakePos(),
                        taken != null ? taken.type() : RemoteResultType.SUCCESS,
                        taken != null ? taken.extractedCount() : 0,
                        returned,
                        delta
                    ));
            } catch (Exception e) {
                Reference.LOGGER.error("Exchange error for {}: {}",
                        player.getName().getString(), e.getMessage(), e);
                ServerPlayNetworking.send(player,
                    new RemoteExchangeResultPayload(
                        payload.getTakePos(), RemoteResultType.INTERNAL_ERROR, 0, 0, null));
            }
        });
    }

    // 对 36 个主背包格拍「计数快照」，只记录 count（0-127），不记 itemId（省带宽）
    private static byte[] snapshotInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        byte[] snap = new byte[36];
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            int count = s.getCount();
            snap[i] = (byte) Math.min(count, 127);
        }
        return snap;
    }

    // 对比前后快照，找出 count 变了的 slot，产生 SlotSnapshot（slotIndex + itemId + count）
    private static List<RemoteExchangeResultPayload.SlotSnapshot> computeDelta(ServerPlayer player,
                                                                                byte[] before, byte[] after) {
        List<RemoteExchangeResultPayload.SlotSnapshot> delta = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (before[i] != after[i]) {
                ItemStack s = inv.getItem(i);
                String itemId = s.isEmpty() ? "" :
                    //#if MC >= 11903
                    BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                    //#else
                    //$$ net.minecraft.core.Registry.ITEM.getKey(s.getItem()).toString();
                    //#endif
                delta.add(new RemoteExchangeResultPayload.SlotSnapshot(i, itemId, s.getCount()));
            }
        }
        return delta;
    }

    // 从玩家背包逐格取出目标物品塞回容器：先 simulateInsert 算能塞多少，再实际 split + insertIntoContainer
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
        int maxStack = item.getDefaultInstance().getMaxStackSize();
        int available = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(item)) continue;
            available += s.getCount();
        }
        int canFit = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) {
                canFit += maxStack;
            } else if (s.is(item) && s.getCount() < maxStack) {
                canFit += maxStack - s.getCount();
            }
        }
        return Math.min(available, canFit);
    }

    private static int insertIntoContainer(Container container, ItemStack stack) {
        int maxStack = stack.getMaxStackSize();
        int remaining = stack.getCount();
        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                int put = Math.min(remaining, maxStack);
                ItemStack copy = stack.copy();
                copy.setCount(put);
                container.setItem(i, copy);
                remaining -= put;
            } else if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < maxStack) {
                int put = Math.min(remaining, maxStack - slot.getCount());
                slot.grow(put);
                remaining -= put;
            }
        }
        return stack.getCount() - remaining;
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.handler;
//$$
//$$ import me.aleksilassila.litematica.printer.container.ContainerItemResolver;
//$$ import me.aleksilassila.litematica.printer.enums.RemoteResultType;
//$$ import me.aleksilassila.litematica.printer.network.payload.RemoteExchangeResultPayload;
//$$ import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//$$ import net.minecraft.server.MinecraftServer;
//$$ import net.minecraft.server.level.ServerPlayer;
//$$
//$$ public class RemoteExchangeHandler {
//$$     public static void register() {}
//$$ }
//#endif
