package me.aleksilassila.litematica.printer.utils;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.EasyPlaceUtilsAccessor;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.InventoryUtilsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


//#if MC >= 12105
import net.minecraft.network.HashedStack;
//#endif

import java.util.List;

import static fi.dy.masa.malilib.util.InventoryUtils.*;

public class InventoryUtils {
    private static final Minecraft client = Minecraft.getInstance();

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

    public static boolean playerHasAccessToItem(LocalPlayer playerEntity, Item item) {
        return playerHasAccessToItems(playerEntity, item);
    }

    public static boolean playerHasAccessToItems(LocalPlayer playerEntity, Item... items) {
        if (items == null || items.length == 0) return true;
        if (PlayerUtils.getAbilities(playerEntity).mayBuild) return true;
        if (!playerEntity.containerMenu.equals(playerEntity.inventoryMenu)) return false;
        Inventory inventory = playerEntity.getInventory();
        for (Item item : items) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).getItem() == item) {
                    return true;
                }
            }
            me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.lastNeedItemList.add(item);
        }
        return false;
    }

    public static boolean setPickedItemToHand(ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setHotbarSlot(int slot, Inventory inventory) {
        boolean usePacket = Configs.Put.PLACE_USE_PACKET.getBooleanValue();
        if (usePacket) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
        setSelectedSlot(inventory, slot);
    }

    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        Player player = mc.player;
        Inventory inventory = player.getInventory();

        if (Inventory.isHotbarSlot(sourceSlot)) {
            setHotbarSlot(sourceSlot, inventory);
            return true;
        } else {
            if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return false;
            }
            int hotbarSlot = sourceSlot;
            // 尝试寻找一个空的可拾取方块的热键栏槽位
            if (sourceSlot == -1 || !Inventory.isHotbarSlot(sourceSlot)) {
                hotbarSlot = InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot(inventory);
            }
            // 如果没有空槽位，则寻找一个可拾取方块的热键栏槽位
            if (hotbarSlot == -1) {
                hotbarSlot = InventoryUtilsAccessor.getPickBlockTargetSlot(player);
            }
            if (hotbarSlot != -1) {
                setHotbarSlot(hotbarSlot, inventory);
                if (EntityUtils.isCreativeMode(player)) {
                    getMainStacks(inventory).set(hotbarSlot, stack.copy());
                    client.gameMode.handleCreativeModeItemAdd(client.player.getMainHandItem(), 36 + hotbarSlot);
                    return true;
                }
                EasyPlaceUtilsAccessor.callSetEasyPlaceLastPickBlockTime();
                return swapItemToMainHand(stack.copy(), mc);
            } else {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
                return false;
            }
        }
    }

    public static boolean swapItemToMainHand(ItemStack stackReference, Minecraft mc) {
        Player player = mc.player;
        if (player == null) return false;

        //#if MC > 12004
        boolean b = areStacksEqualIgnoreNbt(stackReference, player.getMainHandItem());
        //#else
        //$$ boolean b = areStacksEqual(stackReference, player.getMainHandItem());
        //#endif
        if (b) {
            return false;
        }

        int slot = findSlotWithItem(player.inventoryMenu, stackReference, true);
        if (slot != -1) {
            ClientPacketListener connection = client.getConnection();
            if (connection == null) {
                return false;
            }
            int currentHotbarSlot = getSelectedSlot(player.getInventory());
            if (Configs.Put.PLACE_USE_PACKET.getBooleanValue()) {
                NonNullList<Slot> slots = player.inventoryMenu.slots;
                int totalSlots = slots.size();
                List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
                for (Slot slotItem : slots) {
                    copies.add(slotItem.getItem().copy());
                }

                //#if MC >= 12105
                Int2ObjectMap<HashedStack> snapshot = new Int2ObjectOpenHashMap<>();
                //#else
                //$$ Int2ObjectMap<ItemStack> snapshot = new Int2ObjectOpenHashMap<>();
                //#endif

                for (int j = 0; j < totalSlots; j++) {
                    ItemStack original = copies.get(j);
                    ItemStack current = slots.get(j).getItem();
                    if (!ItemStack.isSameItem(original, current)) {
                        //#if MC >=12105
                        snapshot.put(j, HashedStack.create(current, connection.decoratedHashOpsGenenerator()));
                        //#else
                        //$$ snapshot.put(j, current.copy());
                        //#endif
                    }
                }

                //#if MC >= 12105
                HashedStack hashedStack = HashedStack.create(player.inventoryMenu.getCarried(), connection.decoratedHashOpsGenenerator());
                connection.send(new ServerboundContainerClickPacket(
                        player.inventoryMenu.containerId,
                        player.inventoryMenu.getStateId(),
                        Shorts.checkedCast(slot),
                        SignedBytes.checkedCast(currentHotbarSlot),
                        ClickType.SWAP,
                        snapshot,
                        hashedStack
                ));
                //#else
                //$$  connection.send(new ServerboundContainerClickPacket(
                //$$           player.inventoryMenu.containerId,
                //$$           player.inventoryMenu.getStateId(),
                //$$           slot,
                //$$           currentHotbarSlot,
                //$$           ClickType.SWAP,
                //$$           player.inventoryMenu.getCarried().copy(),
                //$$           snapshot
                //$$   ));
                //#endif

                player.inventoryMenu.clicked(slot, currentHotbarSlot, ClickType.SWAP, player);
            } else {
                if (client.gameMode != null) {
                    client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, currentHotbarSlot, ClickType.SWAP, player);
                }
            }
            return true;
        }
        return false;
    }
}
