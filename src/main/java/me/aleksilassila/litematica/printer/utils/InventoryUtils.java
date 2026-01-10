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
    // 副手固定槽位索引（全版本通用，核心常量）
    private static final int OFFHAND_SLOT_INDEX = 40;

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


    // ========== 新增：副手核心方法（无选中格子逻辑） ==========

    /**
     * 获取玩家副手的物品栈（全版本通用，极简实现）
     * @param player 玩家实例
     * @return 副手物品栈
     */
    public static ItemStack getOffhandStack(Player player) {
        // 直接通过固定槽位40获取，无版本专属字段依赖
        return player.getInventory().getItem(OFFHAND_SLOT_INDEX);
    }

    /**
     * 将指定物品切换/设置到副手（核心方法，无选中格子逻辑）
     * @param stack 要放到副手的物品栈
     * @param mc Minecraft实例
     * @return 是否切换成功
     */
    public static boolean setItemToOffhand(ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        Player player = mc.player;

        // 1. 检查副手已有该物品，直接返回成功（避免重复操作）
        boolean isAlreadyInOffhand = areStacksEqual(stack, getOffhandStack(player));
        if (isAlreadyInOffhand) {
            return true;
        }

        // 2. 创造模式：直接设置副手物品（无需交换）
        if (EntityUtils.isCreativeMode(player)) {
            player.getInventory().setItem(OFFHAND_SLOT_INDEX, stack.copy());
            client.gameMode.handleCreativeModeItemAdd(getOffhandStack(player), OFFHAND_SLOT_INDEX);
            return true;
        }

        // 3. 生存模式：找到物品所在槽位，交换到副手
        int sourceSlot = findSlotWithItem(player.inventoryMenu, stack, true);
        if (sourceSlot == -1) {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            return false;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        // 4. 发送交换数据包（复用原有配置，目标槽位固定为40）
        if (Configs.Put.PLACE_USE_PACKET.getBooleanValue()) {
            NonNullList<Slot> slots = player.inventoryMenu.slots;
            int totalSlots = slots.size();
            List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
            for (Slot slotItem : slots) {
                copies.add(slotItem.getItem().copy());
            }

            // 版本兼容的快照对象
            //#if MC >= 12105
            Int2ObjectMap<HashedStack> snapshot = new Int2ObjectOpenHashMap<>();
            //#else
            //$$ Int2ObjectMap<ItemStack> snapshot = new Int2ObjectOpenHashMap<>();
            //#endif

            // 构建库存快照
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

            // 发送SWAP数据包到副手槽位40
            //#if MC >= 12105
            HashedStack hashedStack = HashedStack.create(player.inventoryMenu.getCarried(), connection.decoratedHashOpsGenenerator());
            connection.send(new ServerboundContainerClickPacket(
                    player.inventoryMenu.containerId,
                    player.inventoryMenu.getStateId(),
                    Shorts.checkedCast(sourceSlot),
                    SignedBytes.checkedCast(OFFHAND_SLOT_INDEX), // 目标：副手槽位40
                    ClickType.SWAP,
                    snapshot,
                    hashedStack
            ));
            //#else
            //$$ connection.send(new ServerboundContainerClickPacket(
            //$$         player.inventoryMenu.containerId,
            //$$         player.inventoryMenu.getStateId(),
            //$$         sourceSlot,
            //$$         OFFHAND_SLOT_INDEX, // 目标：副手槽位40
            //$$         ClickType.SWAP,
            //$$         player.inventoryMenu.getCarried().copy(),
            //$$         snapshot
            //$$ ));
            //#endif

            // 本地同步交换操作
            player.inventoryMenu.clicked(sourceSlot, OFFHAND_SLOT_INDEX, ClickType.SWAP, player);
        } else {
            // 不使用数据包：本地直接交换到副手
            if (client.gameMode != null) {
                client.gameMode.handleInventoryMouseClick(
                        player.inventoryMenu.containerId,
                        sourceSlot,
                        OFFHAND_SLOT_INDEX, // 目标：副手槽位40
                        ClickType.SWAP,
                        player
                );
            }
        }

        return true;
    }
}
