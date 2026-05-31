package me.aleksilassila.litematica.printer.utils;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
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
import net.minecraft.world.item.Items;

//#if MC >= 12105
import net.minecraft.network.HashedStack;
//#endif

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryUtils {
    private static final Minecraft client = Minecraft.getInstance();
    private static final int OFFHAND_SLOT_INDEX = 40;
    private static final long MESSAGE_COOLDOWN_MS = 5000L;
    private static final Map<String, Long> LAST_MESSAGE_SEND_TIME = new ConcurrentHashMap<>();
    @Getter
    @Setter
    private static ItemStack orderlyStoreItem;

    @Getter @Setter
    private static boolean isOpenHandler;
    private static int shulkerCooldown;
    private static int shulkerBoxSlot = -1;
    private static final Set<Item> lastNeedItemList = new HashSet<>();

    public static void tick() {
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }
    } //有序存放临时存储

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
        }
        return false;
    }

    public static boolean setPickedItemToHand(ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setHotbarSlot(int slot, Inventory inventory) {
        boolean usePacket = Configs.Placement.PRINT_USE_PACKET.getBooleanValue();
        if (usePacket) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
        setSelectedSlot(inventory, slot);
    }

    /**
     * 检查是否有可用的 Pick 槽位
     *
     * @param sourceSlot 源槽位（-1表示自动寻找）
     * @param mc         Minecraft实例
     * @return PickResult 枚举结果
     */
    public static PickResult checkPickSlotAvailable(int sourceSlot, Minecraft mc) {
        // 基础校验失败 → 返回通用FAIL
        if (mc.player == null) return PickResult.FAIL;
        Player player = mc.player;
        Inventory inventory = player.getInventory();
        // 源槽位是快捷栏 → 成功
        if (Inventory.isHotbarSlot(sourceSlot)) return PickResult.SUCCESS;
        // 无配置可拾取槽位 → 精准失败类型
        if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
            return PickResult.FAIL_NO_PICK_SLOTS_CONFIGURED;
        }
        // 寻找可用槽位
        int hotbarSlot = sourceSlot;
        if (sourceSlot == -1 || !Inventory.isHotbarSlot(sourceSlot)) {
            hotbarSlot = InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot(inventory);
        }
        if (hotbarSlot == -1) {
            hotbarSlot = InventoryUtilsAccessor.getPickBlockTargetSlot(player);
        }
        // 无可用槽位 → 精准失败类型；否则成功
        return hotbarSlot != -1 ? PickResult.SUCCESS : PickResult.FAIL_NO_SUITABLE_SLOT_FOUND;
    }

    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        Player player = mc.player;
        Inventory inventory = player.getInventory();
        // 目标物品在热键栏中
        if (Inventory.isHotbarSlot(sourceSlot)) {
            setHotbarSlot(sourceSlot, inventory);
            return true;
        }
        if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
            showMessageWithCooldown(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
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
            showMessageWithCooldown(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            return false;
        }
    }

    public static boolean swapItemToMainHand(ItemStack stackReference, Minecraft mc) {
        Player player = mc.player;
        if (player == null) return false;

        //#if MC > 12004
        boolean b = fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(stackReference, player.getMainHandItem());
        //#else
        //$$ boolean b = fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual(stackReference, player.getMainHandItem());
        //#endif
        if (b) {
            return false;
        }

        int slot = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.inventoryMenu, stackReference, true);
        if (slot != -1) {
            ClientPacketListener connection = client.getConnection();
            if (connection == null) {
                return false;
            }
            int currentHotbarSlot = getSelectedSlot(player.getInventory());
            if (Configs.Placement.PRINT_USE_PACKET.getBooleanValue()) {
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

    /**
     * 获取玩家副手的物品栈（全版本通用，极简实现）
     *
     * @param player 玩家实例
     * @return 副手物品栈
     */
    public static ItemStack getOffhandStack(Player player) {
        // 直接通过固定槽位40获取，无版本专属字段依赖
        return player.getInventory().getItem(OFFHAND_SLOT_INDEX);
    }

    /**
     * 将指定物品切换/设置到副手（核心方法，无选中格子逻辑）
     *
     * @param stack 要放到副手的物品栈
     * @param mc    Minecraft实例
     * @return 是否切换成功
     */
    public static boolean setItemToOffhand(ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        Player player = mc.player;

        // 1. 检查副手已有该物品，直接返回成功（避免重复操作）
        boolean isAlreadyInOffhand = fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual(stack, getOffhandStack(player));
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
        int sourceSlot = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.inventoryMenu, stack, true);
        if (sourceSlot == -1) {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            return false;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        // 4. 发送交换数据包（复用原有配置，目标槽位固定为40）
        if (Configs.Placement.PRINT_USE_PACKET.getBooleanValue()) {
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


    // ========== 新增：副手核心方法（无选中格子逻辑） ==========

    private static void showMessageWithCooldown(Message.MessageType type, String messageKey) {
        long currentTime = System.currentTimeMillis();
        // 核心修改：通过消息Key获取最后发送时间，而非消息类型
        long lastSendTime = LAST_MESSAGE_SEND_TIME.getOrDefault(messageKey, 0L);

        // 未超过冷却时间，直接返回不发送
        if (currentTime - lastSendTime < MESSAGE_COOLDOWN_MS) {
            return;
        }

        // 超过冷却时间，发送消息并更新【该Key】的最后发送时间
        InfoUtils.showGuiOrInGameMessage(type, messageKey);
        LAST_MESSAGE_SEND_TIME.put(messageKey, currentTime);
    }

    public static boolean switchToItems(LocalPlayer player, Item[] items) {
        if (items == null || items.length == 0) {
            items = new Item[]{Items.AIR};
        }
        Inventory inventory = player.getInventory();
        boolean isCreativeMode = PlayerUtils.getAbilities(player).instabuild;
        if (isCreativeMode) {
            ItemStack stack = new ItemStack(items[0]);
            return InventoryUtils.setPickedItemToHand(stack, client);
        }
        // 有序存放：尝试将手持物品放回原位
        if (Configs.Print.STORE_ORDERLY.getBooleanValue()) {
            returnItemToOriginalSlot(inventory);
        }
        for (Item item : items) {
            int slot = findItemInInventory(inventory, item);
            if (slot != -1) {
                ItemStack itemStack = inventory.getItem(slot);
                orderlyStoreItem = itemStack.copy();
                return InventoryUtils.setPickedItemToHand(slot, itemStack, client);
            }
        }
        if (Configs.Print.USE_QUICK_SHULKER.getBooleanValue()
                && QuickShulkerUtils.isLoaded()) {
            // 没打开，搜索背包里含有目标物品的潜影盒并静默打开
            if (!isOpenHandler && shulkerCooldown <= 0) {
                for (Item item : items) {
                    int shulkerSlot = findShulkerWithItem(player, item);
                    if (shulkerSlot != -1) {
                        ItemStack shulkerStack = inventory.getItem(shulkerSlot);
                        shulkerBoxSlot = shulkerSlot;
                        lastNeedItemList.clear();
                        lastNeedItemList.add(item);
                        ModUtils.closeScreen++;
                        isOpenHandler = true;
                        shulkerCooldown = Configs.Print.SHULKER_COOLDOWN.getIntegerValue();
                        QuickShulkerUtils.openShulker(shulkerStack, shulkerSlot);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private static int findItemInInventory(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int findShulkerWithItem(LocalPlayer player, Item target) {
        // 从背包第9格开始找（跳过热键栏）
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (id.contains("shulker_box") && stack.getCount() == 1) {
                NonNullList<ItemStack> contents = fi.dy.masa.malilib.util.InventoryUtils
                        .getStoredItems(stack, -1);
                if (contents.stream().anyMatch(s -> s.getItem().equals(target))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void returnItemToOriginalSlot(Inventory inventory) {
        if (orderlyStoreItem == null || orderlyStoreItem.isEmpty()) return;
        if (shulkerBoxSlot == -1) return;
        // 检查背包是否已满
        if (hasEmptySlot(inventory)) return;
        int heldSlot = getSelectedSlot(inventory);
        if (!Inventory.isHotbarSlot(heldSlot)) return;
        ItemStack held = inventory.getItem(heldSlot);
        if (held.isEmpty()) return;
        // 把手持物品放回潜影盒所在槽位
        if (inventory.getItem(shulkerBoxSlot).isEmpty()) {
            inventory.setItem(shulkerBoxSlot, held.copy());
            inventory.setItem(heldSlot, ItemStack.EMPTY);
            orderlyStoreItem = null;
        }
    }

    private static boolean hasEmptySlot(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!Inventory.isHotbarSlot(i) && inventory.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static void switchFromShulker() {
        LocalPlayer player = client.player;
        if (player == null || player.containerMenu.equals(player.inventoryMenu)) {
            isOpenHandler = false;
            return;
        }
        for (Slot slot : player.containerMenu.slots) {
            if (!slot.hasItem()) continue;
            for (Item item : lastNeedItemList) {
                if (slot.getItem().getItem().equals(item)) {
                    int hotbarSlot = InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot(player.getInventory());
                    if (hotbarSlot == -1) {
                        hotbarSlot = InventoryUtilsAccessor.getPickBlockTargetSlot(player);
                    }
                    if (hotbarSlot != -1) {
                        fi.dy.masa.malilib.util.InventoryUtils.swapSlots(
                                player.containerMenu, slot.index, hotbarSlot);
                        setSelectedSlot(player.getInventory(), hotbarSlot);
                        // 同步到服务端：点击潜影盒在背包中的槽位来刷新状态
                        if (shulkerBoxSlot != -1) {
                            client.gameMode.handleInventoryMouseClick(
                                    player.containerMenu.containerId,
                                    shulkerBoxSlot, 0, ClickType.PICKUP, client.player);
                            client.gameMode.handleInventoryMouseClick(
                                    player.containerMenu.containerId,
                                    shulkerBoxSlot, 0, ClickType.PICKUP, client.player);
                        }
                    }
                    player.closeContainer();
                    shulkerBoxSlot = -1;
                    isOpenHandler = false;
                    lastNeedItemList.clear();
                    return;
                }
            }
        }
        player.closeContainer();
        isOpenHandler = false;
        lastNeedItemList.clear();
    }

    public PickResult checkCanSwitchToItems(LocalPlayer player, Item[] items) {
        if (player == null) {
            return PickResult.FAIL;
        }
        Item[] targetItems = items;
        if (targetItems == null || targetItems.length == 0) {
            targetItems = new Item[]{Items.AIR};
        }
        Inventory inv = player.getInventory();
        boolean isCreativeMode = PlayerUtils.getAbilities(player).instabuild;
        if (isCreativeMode) {
            return InventoryUtils.checkPickSlotAvailable(-1, client);
        }
        for (Item item : targetItems) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack itemStack = inv.getItem(i);
                if (itemStack.getItem().equals(item)) {
                    return InventoryUtils.checkPickSlotAvailable(i, client);
                }
            }
        }
        return PickResult.FAIL;
    }

    public enum PickResult {
        SUCCESS,
        FAIL,
        FAIL_NO_PICK_SLOTS_CONFIGURED,
        FAIL_NO_SUITABLE_SLOT_FOUND;

        public boolean isNoPickSlotsConfigured() {
            return this == FAIL_NO_PICK_SLOTS_CONFIGURED;
        }

        public boolean isNoSuitableSlotFound() {
            return this == FAIL_NO_SUITABLE_SLOT_FOUND;
        }

        public boolean isNoAvailableSlot() {
            return isNoPickSlotsConfigured() || isNoSuitableSlotFound();
        }

        public boolean isAvailable() {
            return this == SUCCESS;
        }
    }
}
