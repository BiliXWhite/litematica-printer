package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.EasyPlaceUtilsAccessor;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.InventoryUtilsAccessor;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.integration.litematica.LitematicaPickSlotAdapter;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.ToolSelectionUtils;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;

import static fi.dy.masa.malilib.util.InventoryUtils.*;

@SuppressWarnings({"DataFlowIssue", "SpellCheckingInspection", "GrazieInspection"})
public class InventoryUtils {
    private static final Minecraft client = Minecraft.getInstance();
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

    public static boolean playerHasItemInInventory(LocalPlayer playerEntity, Item item) {
        return playerHasItemInInventory(playerEntity, item, stack -> true);
    }

    public static boolean playerHasItemInInventory(LocalPlayer playerEntity, Item item,
                                                    java.util.function.Predicate<ItemStack> predicate) {
        if (playerEntity == null || item == null) {
            return false;
        }
        if (PlayerUtils.getAbilities(playerEntity).instabuild) {
            return true;
        }
        Inventory inventory = playerEntity.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item) && predicate.test(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    public static boolean playerHasAccessToItems(LocalPlayer playerEntity, Item... items) {
        if (items == null || items.length == 0) return true;
        if (playerEntity == null) return false;
        if (PlayerUtils.getAbilities(playerEntity).instabuild) return true;
        if (!playerEntity.containerMenu.equals(playerEntity.inventoryMenu)) return false;
        Inventory inventory = playerEntity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            Item inventoryItem = inventory.getItem(i).getItem();
            for (Item item : items) {
                if (inventoryItem == item) {
                    return true;
                }
            }
        }
        QuickShulkerBridge.requestItems(items, MaterialRequest.Source.PRINT);
        return false;
    }

    public static boolean setPickedItemToHand(ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setHotbarSlot(int slot, Inventory inventory) {
        setSelectedSlot(inventory, slot);
        syncSelectedHotbarSlot();
    }

    public static void syncSelectedHotbarSlot() {
        LocalPlayer player = client.player;
        ClientPacketListener connection = client.getConnection();
        if (player == null || connection == null) {
            return;
        }
        connection.send(new ServerboundSetCarriedItemPacket(getSelectedSlot(player.getInventory())));
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
            hotbarSlot = LitematicaPickSlotAdapter.selectNextAvailable(player);
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
            hotbarSlot = LitematicaPickSlotAdapter.selectNextAvailable(player);
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
        boolean b = areStacksEqualIgnoreNbt(stackReference, player.getMainHandItem());
        //#else
        //$$ boolean b = areStacksEqual(stackReference, player.getMainHandItem());
        //#endif
        if (b) {
            return false;
        }

        int slot = findSlotWithItem(player.inventoryMenu, stackReference, true);
        if (slot != -1) {
            if (client.gameMode == null) {
                return false;
            }
            int currentHotbarSlot = getSelectedSlot(player.getInventory());
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, currentHotbarSlot, ContainerInput.SWAP, player);
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

        if (client.gameMode == null) {
            return false;
        }
        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId,
                sourceSlot,
                OFFHAND_SLOT_INDEX,
                ContainerInput.SWAP,
                player
        );

        return true;
    }


    // ========== 新增：副手核心方法（无选中格子逻辑） ==========

    private static void showMessageWithCooldown(Message.MessageType type, String messageKey) {
        long currentTime = System.currentTimeMillis();
        if (!RuntimeAccess.get().inventoryMessageCooldown().shouldSend(messageKey, currentTime)) {
            return;
        }
        InfoUtils.showGuiOrInGameMessage(type, messageKey);
    }

    public static boolean switchToBestTool(LocalPlayer player, BlockState blockState) {
        ClientLevel level = client.level;
        BlockPos pos = player == null ? null : player.blockPosition();
        return ToolInventorySelector.switchToBestTool(client, player, blockState, level, pos);
    }

    public static boolean switchToBestTool(LocalPlayer player, BlockState blockState, BlockPos pos) {
        return ToolInventorySelector.switchToBestTool(client, player, blockState, client.level, pos);
    }

    public static boolean hasUsableSilkTouchTool(LocalPlayer player) {
        if (player == null || PlayerUtils.getAbilities(player).instabuild) {
            return false;
        }
        for (ItemStack stack : getMainStacks(player.getInventory())) {
            if (!stack.isEmpty() && ToolSelectionUtils.hasSilkTouch(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean switchToItems(LocalPlayer player, Item[] items) {
        return switchToItems(player, items, -1);
    }

    public static boolean switchToItemsWithReserve(LocalPlayer player, Item[] items, int reserveCount) {
        return switchToItems(player, items, Math.max(0, reserveCount));
    }

    private static boolean switchToItems(LocalPlayer player, Item[] items, int reserveCount) {
        return MaterialSelector.switchToItems(player, items, reserveCount);
    }

    public static boolean playerHasAccessToMatchingStack(
            LocalPlayer playerEntity,
            ItemStack creativeFallback,
            Predicate<ItemStack> predicate
    ) {
        if (playerEntity == null || predicate == null) {
            return false;
        }
        if (PlayerUtils.getAbilities(playerEntity).instabuild) {
            return creativeFallback != null && predicate.test(creativeFallback);
        }
        if (!playerEntity.containerMenu.equals(playerEntity.inventoryMenu)) {
            return false;
        }
        Inventory inventory = playerEntity.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHoldingAnyItem(LocalPlayer player, Item[] items) {
        if (items == null || items.length == 0) {
            return true;
        }
        Item heldItem = player.getMainHandItem().getItem();
        for (Item item : items) {
            if (heldItem.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean switchToMatchingStack(
            LocalPlayer player,
            Predicate<ItemStack> predicate,
            ItemStack creativeFallback
    ) {
        return switchToMatchingStack(player, predicate, creativeFallback, -1);
    }

    public static boolean switchToMatchingStackWithReserve(
            LocalPlayer player,
            Predicate<ItemStack> predicate,
            ItemStack creativeFallback,
            int reserveCount
    ) {
        return switchToMatchingStack(player, predicate, creativeFallback, Math.max(0, reserveCount));
    }

    private static boolean switchToMatchingStack(
            LocalPlayer player,
            Predicate<ItemStack> predicate,
            ItemStack creativeFallback,
            int reserveCount
    ) {
        return MaterialSelector.switchToMatchingStack(player, predicate, creativeFallback, reserveCount);
    }

    /**
     * 返回当前物品最多还能安全尝试消耗的次数。
     * {@link Integer#MAX_VALUE} 表示该物品不受保留数量限制。
     */
    public static int getConsumableSurplus(
            LocalPlayer player,
            ItemStack stack,
            @Nullable Predicate<ItemStack> requiredStackPredicate,
            int reserveCount
    ) {
        return MaterialSelector.getConsumableSurplus(player, stack, requiredStackPredicate, reserveCount);
    }

    public static ItemStack findReserveBlockedStack(
            LocalPlayer player,
            Item[] items,
            @Nullable Predicate<ItemStack> requiredStackPredicate,
            int reserveCount
    ) {
        return MaterialSelector.findReserveBlockedStack(player, items, requiredStackPredicate, reserveCount);
    }

    public enum PickResult {
        SUCCESS,
        FAIL,
        FAIL_NO_PICK_SLOTS_CONFIGURED,
        FAIL_NO_SUITABLE_SLOT_FOUND;

        // 快捷判断：是否是「未配置可拾取槽位」
        public boolean isNoPickSlotsConfigured() {
            return this == FAIL_NO_PICK_SLOTS_CONFIGURED;
        }

        // 快捷判断：是否是「无可用槽位」
        public boolean isNoSuitableSlotFound() {
            return this == FAIL_NO_SUITABLE_SLOT_FOUND;
        }

        // 快捷方法：是否「无可用槽位」（包含两种精准失败类型）
        public boolean isNoAvailableSlot() {
            return isNoPickSlotsConfigured() || isNoSuitableSlotFound();
        }

        // 快捷方法：是否「有可用槽位」（仅SUCCESS表示有）
        public boolean isAvailable() {
            return this == SUCCESS;
        }
    }
}
