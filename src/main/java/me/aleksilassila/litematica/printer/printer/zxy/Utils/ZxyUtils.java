package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PreprocessUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;

//#if MC >= 12001 && MC <= 12104
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#elseif MC < 12001
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//#endif

import static me.aleksilassila.litematica.printer.config.Configs.SYNC_INVENTORY_CHECK;
import static me.aleksilassila.litematica.printer.config.Configs.SYNC_INVENTORY_COLOR;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.*;
import static me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus.closeScreen;
import static net.minecraft.world.level.block.ShulkerBoxBlock.FACING;

public class ZxyUtils {
    //旧版箱子追踪
    @SuppressWarnings("unused")
    public static boolean qw = false;
    @SuppressWarnings("unused")
    public static int currWorldId = 0;

    @NotNull
    static Minecraft client = Minecraft.getInstance();
    public static LinkedList<BlockPos> invBlockList = new LinkedList<>();
    public static boolean printerMemoryAdding = false;
    @SuppressWarnings("unused")
    public static boolean syncPrinterInventory = false;
    public static String syncInventoryId = "syncInventory";

    private static int sequence = 0;

    public static void startAddPrinterInventory() {
        getReadyColor();
        if (Configs.CLOUD_INVENTORY.getBooleanValue() && !printerMemoryAdding) {
            printerMemoryAdding = true;
            //#if MC >= 12001 && MC <= 12104
            //$$ if (MemoryUtils.PRINTER_MEMORY == null) MemoryUtils.createPrinterMemory();
            //#endif

            for (String string : Configs.INVENTORY_LIST.getStrings()) {
                invBlockList.addAll(filterBlocksByName(string).stream().filter(InventoryUtils::canOpenInv).toList());
            }
            highlightPosList.addAll(invBlockList);
        }
    }

    public static void addInv() {
        if (printerMemoryAdding && !openIng && OpenInventoryPacket.key == null) {
            if (invBlockList.isEmpty()) {
                printerMemoryAdding = false;
                client.gui.setOverlayMessage(Component.nullToEmpty("打印机库存添加完成"), false);
                return;
            }
            client.gui.setOverlayMessage(Component.nullToEmpty("添加库存中"), false);
            for (BlockPos pos : invBlockList) {
                if (client.level != null) {
                    //#if MC < 12001
                    //$$ MemoryUtils.setLatestPos(pos);
                    //#endif
                    closeScreen++;
                    OpenInventoryPacket.sendOpenInventory(pos, client.level.dimension());
                }
                invBlockList.remove(pos);
                highlightPosList.remove(pos);
                break;
            }
        }
    }

    public static LinkedList<BlockPos> syncPosList = new LinkedList<>();
    public static ArrayList<ItemStack> targetBlockInv;
    public static int num = 0;
    static BlockPos blockPos = null;
    static Set<BlockPos> highlightPosList = new LinkedHashSet<>();
    static Map<ItemStack, Integer> targetItemsCount = new HashMap<>();
    static Map<ItemStack, Integer> playerItemsCount = new HashMap<>();

    private static void getReadyColor() {
        HighlightBlockRenderer.createHighlightBlockList(syncInventoryId, SYNC_INVENTORY_COLOR);
        highlightPosList = HighlightBlockRenderer.getHighlightBlockPosList(syncInventoryId);
    }

    public static void startOrOffSyncInventory() {
        getReadyColor();
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK && syncPosList.isEmpty()) {
            BlockPos pos = ((BlockHitResult) client.hitResult).getBlockPos();
            BlockState blockState = client.level.getBlockState(pos);
            Block block = null;
            if (client.level != null) {
                block = client.level.getBlockState(pos).getBlock();
                BlockEntity blockEntity = client.level.getBlockEntity(pos);
                boolean isInventory = InventoryUtils.isInventory(client.level, pos);
                try {
                    if ((isInventory && blockState.getMenuProvider(client.level, pos) == null) ||
                            (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                    //#if MC > 12103
                                    !client.level.noCollision(Shulker.getProgressDeltaAabb(1.0F, blockState.getValue(FACING), 0.0F, 0.5F, pos.getBottomCenter()).move(pos).deflate(1.0E-6)) &&
                                    //#elseif MC <= 12103 && MC > 12004
                                    //$$ !client.level.noCollision(Shulker.getProgressDeltaAabb(1.0F, blockState.getValue(FACING), 0.0F, 0.5F).move(pos).deflate(1.0E-6)) &&
                                    //#elseif MC <= 12004
                                    //$$ !client.level.noCollision(Shulker.getProgressDeltaAabb(blockState.getValue(FACING), 0.0f, 0.5f).move(pos).deflate(1.0E-6)) &&
                                    //#endif
                                    entity.getAnimationStatus() == ShulkerBoxBlockEntity.AnimationStatus.CLOSED)) {
                        client.gui.setOverlayMessage(Component.nullToEmpty("容器无法打开"), false);
                    } else if (!isInventory) {
                        client.gui.setOverlayMessage(Component.nullToEmpty("这不是容器 无法同步"), false);
                        return;
                    }
                } catch (Exception e) {
                    client.gui.setOverlayMessage(Component.nullToEmpty("这不是容器 无法同步"), false);
                    return;
                }
            }
            String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();
            syncPosList.addAll(filterBlocksByName(blockName));
            if (!syncPosList.isEmpty()) {
                if (client.player == null) return;
                client.player.closeContainer();
                if (!openInv(pos, false)) {
                    syncPosList = new LinkedList<>();
                    return;
                }
                highlightPosList.addAll(syncPosList);
                closeScreen++;
                num = 1;
            }
        } else if (!syncPosList.isEmpty()) {
            syncPosList.forEach(highlightPosList::remove);
            syncPosList = new LinkedList<>();
            if (client.player != null) client.player.clientSideCloseContainer();
            num = 0;
            client.gui.setOverlayMessage(Component.nullToEmpty("已取消同步"), false);
        }
    }

    public static boolean openInv(BlockPos pos, boolean ignoreThePrompt) {
        if (Configs.CLOUD_INVENTORY.getBooleanValue() && OpenInventoryPacket.key == null) {
            OpenInventoryPacket.sendOpenInventory(pos, client.level.dimension());
            return true;
        } else {
            if (client.player != null && !PlayerUtils.canInteracted(pos)) {
                if (!ignoreThePrompt)
                    client.gui.setOverlayMessage(Component.nullToEmpty("距离过远无法打开容器"), false);
                return false;
            }
            if (client.gameMode != null) {
                //#if MC < 11904
                //$$ client.gameMode.useItemOn(client.player, client.level, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction. DOWN, pos, false));
                //#else
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false));
                //#endif
                return true;
            } else return false;
        }
    }

    public static void itemsCount(Map<ItemStack, Integer> itemsCount, ItemStack itemStack) {
        // 判断是否存在可合并的键
        Optional<Map.Entry<ItemStack, Integer>> entry = itemsCount.entrySet().stream()
                .filter(e -> ItemStack.isSameItemSameComponents(e.getKey(), itemStack))
                .findFirst();

        if (entry.isPresent()) {
            // 更新已有键对应的值
            Integer count = entry.get().getValue();
            count += itemStack.getCount();
            itemsCount.put(entry.get().getKey(), count);
        } else {
            // 添加新键值对
            itemsCount.put(itemStack, itemStack.getCount());
        }
    }

    public static void syncInv() {
        switch (num) {
            case 1 -> {
                //按下热键后记录看向的容器 开始同步容器 只会触发一次
                targetBlockInv = new ArrayList<>();
                targetItemsCount = new HashMap<>();
                if (client.player != null && (!Configs.CLOUD_INVENTORY.getBooleanValue() || openIng) && !client.player.containerMenu.equals(client.player.inventoryMenu)) {
                    for (int i = 0; i < client.player.containerMenu.slots.get(0).container.getContainerSize(); i++) {
                        ItemStack copy = client.player.containerMenu.slots.get(i).getItem().copy();
                        itemsCount(targetItemsCount, copy);
                        targetBlockInv.add(copy);
                    }
                    //上面如果不使用copy()在关闭容器后会使第一个元素号变该物品成总数 非常有趣...
//                    System.out.println("???1 "+targetBlockInv.get(0).getCount());
                    client.player.closeContainer();
//                    System.out.println("!!!1 "+targetBlockInv.get(0).getCount());
                    num = 2;
                }
            }
            case 2 -> {
                //打开列表中的容器 只要容器同步列表不为空 就会一直执行此处
                if (client.player == null) return;
                playerItemsCount = new HashMap<>();
                client.gui.setOverlayMessage(Component.nullToEmpty("剩余 " + syncPosList.size() + " 个容器. 再次按下快捷键取消同步"), false);
                if (!client.player.containerMenu.equals(client.player.inventoryMenu)) return;
                NonNullList<Slot> slots = client.player.inventoryMenu.slots;
                slots.forEach(slot -> itemsCount(playerItemsCount, slot.getItem()));

                if (SYNC_INVENTORY_CHECK.getBooleanValue() && !targetItemsCount.entrySet().stream()
                        .allMatch(target -> playerItemsCount.entrySet().stream()
                                .anyMatch(player ->
                                        ItemStack.isSameItemSameComponents(player.getKey(), target.getKey()) && target.getValue() <= player.getValue())))
                    return;

                if ((!Configs.CLOUD_INVENTORY.getBooleanValue() || !openIng) && OpenInventoryPacket.key == null) {
                    for (BlockPos pos : syncPosList) {
                        if (!openInv(pos, true)) continue;
                        closeScreen++;
                        blockPos = pos;
                        num = 3;
                        break;
                    }
                }
                if (syncPosList.isEmpty()) {
                    num = 0;
                    client.gui.setOverlayMessage(Component.nullToEmpty("同步完成"), false);
                }
            }
            case 3 -> {
                //开始同步 在打开容器后触发
                AbstractContainerMenu sc = client.player.containerMenu;
                if (sc.equals(client.player.inventoryMenu)) return;
                int size = Math.min(targetBlockInv.size(), sc.slots.get(0).container.getContainerSize());

                int times = 0;
                for (int i = 0; i < size; i++) {
                    ItemStack item1 = sc.slots.get(i).getItem();
                    ItemStack item2 = targetBlockInv.get(i).copy();
                    int currNum = item1.getCount();
                    int tarNum = item2.getCount();
                    boolean same = ItemStack.isSameItemSameComponents(item1, item2.copy()) && !item1.isEmpty();
                    if (ItemStack.isSameItemSameComponents(item1, item2) && currNum == tarNum) continue;
                    //不和背包交互
                    if (same) {
                        //有多
                        while (currNum > tarNum) {
                            client.gameMode.handleInventoryMouseClick(sc.containerId, i, 0, ClickType.THROW, client.player);
                            currNum--;
                        }
                    } else {
                        //不同直接扔出
                        client.gameMode.handleInventoryMouseClick(sc.containerId, i, 1, ClickType.THROW, client.player);
                        times++;
                    }
                    boolean thereAreItems = false;
                    //背包交互
                    for (int i1 = size; i1 < sc.slots.size(); i1++) {
                        ItemStack stack = sc.slots.get(i1).getItem();
                        ItemStack currStack = sc.slots.get(i).getItem();
                        currNum = currStack.getCount();
                        boolean same2 = thereAreItems = ItemStack.isSameItemSameComponents(item2, stack);
                        if (same2 && !stack.isEmpty()) {
                            int i2 = stack.getCount();
                            client.gameMode.handleInventoryMouseClick(sc.containerId, i1, 0, ClickType.PICKUP, client.player);
                            for (; currNum < tarNum && i2 > 0; i2--) {
                                client.gameMode.handleInventoryMouseClick(sc.containerId, i, 1, ClickType.PICKUP, client.player);
                                currNum++;
                            }
                            client.gameMode.handleInventoryMouseClick(sc.containerId, i1, 0, ClickType.PICKUP, client.player);
                        }
                        //这里判断没啥用，因为一个游戏刻操作背包太多次.getStack().getCount()获取的数量不准确 下次一定优化，
                        if (currNum != tarNum) times++;
                    }
                    if (!thereAreItems) times++;
                }
                if (times == 0) {
                    syncPosList.remove(blockPos);
                    highlightPosList.remove(blockPos);
                    blockPos = null;
                }
                client.player.closeContainer();
                num = 2;
            }
        }
    }

    public static void tick() {
        if (num == 2) {
            syncInv();
        }
        addInv();
        OpenInventoryPacket.tick();
    }

    public static void switchPlayerInvToHotbarAir(int slot) {
        if (client.player == null) return;
        LocalPlayer player = client.player;
        AbstractContainerMenu sc = player.containerMenu;
        NonNullList<Slot> slots = sc.slots;
        int i = sc.equals(player.inventoryMenu) ? 9 : 0;
        for (; i < slots.size(); i++) {
            if (slots.get(i).getItem().isEmpty() && slots.get(i).container instanceof Inventory) {
                fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, i, slot);
                return;
            }
        }
    }

    public static void exitGameReSet() {
        SwitchItem.reSet();
        isRemote = false;
        clientTry = false;
        remoteTime = 0;
    }

    public static int getSequence() {
        return sequence++;
    }

    private static void selectHotbarSlot(int slot, Inventory inventory, boolean usePacket) {
        if (usePacket) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
        me.aleksilassila.litematica.printer.utils.InventoryUtils.setSelectedSlot(inventory, slot);
    }

    public static void actionBar(String message) {
        client.gui.setOverlayMessage(Component.nullToEmpty(message), false);
    }

    /**
     * 从当前选中的区域中筛选出指定名称的方块，并返回这些方块的位置列表。
     *
     * @param blockName 方块的名字，用于匹配要筛选的方块类型
     * @return 返回一个包含所有匹配到的方块位置的LinkedList。如果没有找到匹配项或当前没有选中任何区域，则返回空列表。
     */
    public static LinkedList<BlockPos> filterBlocksByName(String blockName) {
        LinkedList<BlockPos> blocks = new LinkedList<>();
        AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
        List<Box> boxes;
        if (i == null) return blocks;
        boxes = i.getAllSubRegionBoxes();
        for (Box box : boxes) {
            MyBox myBox = new MyBox(box);
            for (BlockPos pos : myBox) {
                BlockState state = null;
                if (Printer.client.level != null) {
                    state = Printer.client.level.getBlockState(pos);
                }
                if (Filters.equalsName(blockName, state)) {
                    blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    //右键单击
//              client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.PICKUP, client.player);
    //左键单击
//              client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
    //点击背包外
//              client.interactionManager.clickSlot(sc.syncId, -999, 0, SlotActionType.PICKUP, client.player);
    //丢弃一个
//              client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.THROW, client.player);
    //丢弃全部
//              client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.THROW, client.player);
    //开始拖动
//              client.interactionManager.clickSlot(sc.syncId, -999, 0, SlotActionType.QUICK_CRAFT, client.player);
    //拖动经过的槽
//              client.interactionManager.clickSlot(sc.syncId, i1, 1, SlotActionType.QUICK_CRAFT, client.player);
    //结束拖动
//              client.interactionManager.clickSlot(sc.syncId, -999, 2, SlotActionType.QUICK_CRAFT, client.player);
    //副手交换
//              client.interactionManager.clickSlot(sc.syncId, i, 40, SlotActionType.SWAP, client.player);

}
