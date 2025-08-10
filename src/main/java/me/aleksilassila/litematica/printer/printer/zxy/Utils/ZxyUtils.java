package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import com.google.common.collect.Lists;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;
//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#else
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//#endif
//#if MC >= 12006
import net.minecraft.registry.RegistryKey;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.registry.entry.RegistryEntry;
//#endif

//#if MC <= 12006
//$$import net.minecraft.enchantment.EnchantmentHelper;
//#endif

//#if MC >= 12105
//$$ import com.google.common.primitives.Shorts;
//$$ import com.google.common.primitives.SignedBytes;
//$$ import net.minecraft.screen.sync.ItemStackHash;
//#endif

//#if MC > 11802
import net.minecraft.text.MutableText;
//#else
//$$ import net.minecraft.text.TranslatableText;
//#endif
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.SYNC_INVENTORY_CHECK;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.SYNC_INVENTORY_COLOR;
import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.getEmptyPickBlockableHotbarSlot;
import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.getPickBlockTargetSlot;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static net.minecraft.block.ShulkerBoxBlock.FACING;

public class ZxyUtils {
    //旧版箱子追踪
    @SuppressWarnings("unused")
    public static boolean qw = false;
    @SuppressWarnings("unused")
    public static int currWorldId = 0;

    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();
    public static LinkedList<BlockPos> invBlockList = new LinkedList<>();
    public static boolean printerMemoryAdding = false;
    @SuppressWarnings("unused")
    public static boolean syncPrinterInventory = false;
    public static String syncInventoryId = "syncInventory";

    private static int sequence = 0;
    private static boolean isSwitching = true;

    public static void startAddPrinterInventory(){
        getReadyColor();
        if (LitematicaMixinMod.CLOUD_INVENTORY.getBooleanValue() && !printerMemoryAdding) {
            printerMemoryAdding = true;
            //#if MC >= 12001
            if (MemoryUtils.PRINTER_MEMORY == null) MemoryUtils.createPrinterMemory();
            //#endif

            for (String string : LitematicaMixinMod.INVENTORY_LIST.getStrings()) {
                invBlockList.addAll(siftBlock(string).stream().filter(InventoryUtils::canOpenInv).toList());
            }
            highlightPosList.addAll(invBlockList);
        }
    }
    public static void addInv() {
        if (printerMemoryAdding && !openIng && OpenInventoryPacket.key == null) {
            if (invBlockList.isEmpty()) {
                printerMemoryAdding = false;
                client.inGameHud.setOverlayMessage(Text.of("打印机库存添加完成"), false);
                return;
            }
            client.inGameHud.setOverlayMessage(Text.of("添加库存中"), false);
            for (BlockPos pos : invBlockList) {
                if (client.world != null) {
                    //#if MC < 12001
                    //$$ MemoryUtils.setLatestPos(pos);
                    //#endif
                    closeScreen++;
                    OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
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
    static Map<ItemStack,Integer> targetItemsCount = new HashMap<>();
    static Map<ItemStack,Integer> playerItemsCount = new HashMap<>();

    private static void getReadyColor(){
        HighlightBlockRenderer.createHighlightBlockList(syncInventoryId,SYNC_INVENTORY_COLOR);
        highlightPosList = HighlightBlockRenderer.getHighlightBlockPosList(syncInventoryId);
    }

    public static void startOrOffSyncInventory() {
        getReadyColor();
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK && syncPosList.isEmpty()) {
            BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
            BlockState blockState = client.world.getBlockState(pos);
            Block block = null;
            if (client.world != null) {
                block = client.world.getBlockState(pos).getBlock();
                BlockEntity blockEntity = client.world.getBlockEntity(pos);
                boolean isInventory = InventoryUtils.isInventory(client.world,pos);
                try {
                    if ((isInventory && blockState.createScreenHandlerFactory(client.world,pos) == null) ||
                            (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                    //#if MC > 12101
                                    !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F, pos.toBottomCenterPos()).offset(pos).contract(1.0E-6)) &&
                                    //#elseif MC <= 12101 && MC > 12004
                                    //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F).offset(pos).contract(1.0E-6)) &&
                                    //#elseif MC <= 12004
                                    //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                                    //#endif
                                    entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED)) {
                        client.inGameHud.setOverlayMessage(Text.of("容器无法打开"), false);
                    }else if(!isInventory){
                        client.inGameHud.setOverlayMessage(Text.of("这不是容器 无法同步"), false);
                        return;
                    }
                } catch (Exception e) {
                    client.inGameHud.setOverlayMessage(Text.of("这不是容器 无法同步"), false);
                    return;
                }
            }
            String blockName = Registries.BLOCK.getId(block).toString();
            syncPosList.addAll(siftBlock(blockName));
            if (!syncPosList.isEmpty()) {
                if (client.player == null) return;
                client.player.closeHandledScreen();
                if (!openInv(pos,false)){
                    syncPosList = new LinkedList<>();
                    return;
                }
                highlightPosList.addAll(syncPosList);
                closeScreen++;
                num = 1;
            }
        } else if(!syncPosList.isEmpty()){
            syncPosList.forEach(highlightPosList::remove);
            syncPosList = new LinkedList<>();
            if (client.player != null) client.player.closeScreen();
            num = 0;
            client.inGameHud.setOverlayMessage(Text.of("已取消同步"), false);
        }
    }
    public static boolean openInv(BlockPos pos,boolean ignoreThePrompt){
        if(LitematicaMixinMod.CLOUD_INVENTORY.getBooleanValue() && OpenInventoryPacket.key == null) {
            OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
            return true;
        } else {
            if (client.player != null && !canInteracted(Vec3d.ofCenter(pos), LitematicaMixinMod.COMPULSION_RANGE.getIntegerValue())) {
                if(!ignoreThePrompt) client.inGameHud.setOverlayMessage(Text.of("距离过远无法打开容器"), false);
                return false;
            }
            if (client.interactionManager != null){
                //#if MC < 11904
                //$$ client.interactionManager.interactBlock(client.player, client.world, Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN,pos,false));
                //#else
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN,pos,false));
                //#endif
                return true;
            } else return false;
        }
    }
    public static void itemsCount(Map<ItemStack,Integer> itemsCount , ItemStack itemStack){
        // 判断是否存在可合并的键
        Optional<Map.Entry<ItemStack, Integer>> entry = itemsCount.entrySet().stream()
                .filter(e -> ItemStack.areItemsAndComponentsEqual(e.getKey(), itemStack))
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
                if (client.player != null && (!LitematicaMixinMod.CLOUD_INVENTORY.getBooleanValue() || openIng) && !client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) {
                    for (int i = 0; i < client.player.currentScreenHandler.slots.get(0).inventory.size(); i++) {
                        ItemStack copy = client.player.currentScreenHandler.slots.get(i).getStack().copy();
                        itemsCount(targetItemsCount,copy);
                        targetBlockInv.add(copy);
                    }
                    //上面如果不使用copy()在关闭容器后会使第一个元素号变该物品成总数 非常有趣...
//                    System.out.println("???1 "+targetBlockInv.get(0).getCount());
                    client.player.closeHandledScreen();
//                    System.out.println("!!!1 "+targetBlockInv.get(0).getCount());
                    num = 2;
                }
            }
            case 2 -> {
                //打开列表中的容器 只要容器同步列表不为空 就会一直执行此处
                if (client.player == null) return;
                playerItemsCount = new HashMap<>();
                client.inGameHud.setOverlayMessage(Text.of("剩余 " + syncPosList.size() + " 个容器. 再次按下快捷键取消同步"), false);
                if (!client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) return;
                DefaultedList<Slot> slots = client.player.playerScreenHandler.slots;
                slots.forEach(slot -> itemsCount(playerItemsCount,slot.getStack()));

                if (SYNC_INVENTORY_CHECK.getBooleanValue() && !targetItemsCount.entrySet().stream()
                        .allMatch(target -> playerItemsCount.entrySet().stream()
                                .anyMatch(player ->
                                        ItemStack.areItemsAndComponentsEqual(player.getKey(), target.getKey()) && target.getValue() <= player.getValue()))) return;

                if ((!LitematicaMixinMod.CLOUD_INVENTORY.getBooleanValue() || !openIng) && OpenInventoryPacket.key == null) {
                    for (BlockPos pos : syncPosList) {
                        if (!openInv(pos,true)) continue;
                        closeScreen++;
                        blockPos = pos;
                        num = 3;
                        break;
                    }
                }
                if (syncPosList.isEmpty()) {
                    num = 0;
                    client.inGameHud.setOverlayMessage(Text.of("同步完成"), false);
                }
            }
            case 3 -> {
                //开始同步 在打开容器后触发
                ScreenHandler sc = client.player.currentScreenHandler;
                if (sc.equals(client.player.playerScreenHandler)) return;
                int size = Math.min(targetBlockInv.size(),sc.slots.get(0).inventory.size());

                int times = 0;
                for (int i = 0; i < size; i++) {
                    ItemStack item1 = sc.slots.get(i).getStack();
                    ItemStack item2 = targetBlockInv.get(i).copy();
                    int currNum = item1.getCount();
                    int tarNum = item2.getCount();
                    boolean same = ItemStack.areItemsAndComponentsEqual(item1,item2.copy()) && !item1.isEmpty();
                    if(ItemStack.areItemsAndComponentsEqual(item1,item2) && currNum == tarNum) continue;
                    //不和背包交互
                    if (same) {
                        //有多
                        while (currNum > tarNum) {
                            client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.THROW, client.player);
                            currNum--;
                        }
                    } else {
                        //不同直接扔出
                        client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.THROW, client.player);
                        times++;
                    }
                    boolean thereAreItems = false;
                    //背包交互
                    for (int i1 = size; i1 < sc.slots.size(); i1++) {
                        ItemStack stack = sc.slots.get(i1).getStack();
                        ItemStack currStack = sc.slots.get(i).getStack();
                        currNum = currStack.getCount();
                        boolean same2 = thereAreItems = ItemStack.areItemsAndComponentsEqual(item2,stack);
                        if (same2 && !stack.isEmpty()) {
                            int i2 = stack.getCount();
                            client.interactionManager.clickSlot(sc.syncId, i1, 0, SlotActionType.PICKUP, client.player);
                            for (; currNum < tarNum && i2 > 0; i2--) {
                                client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.PICKUP, client.player);
                                currNum++;
                            }
                            client.interactionManager.clickSlot(sc.syncId, i1, 0, SlotActionType.PICKUP, client.player);
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
                client.player.closeHandledScreen();
                num = 2;
            }
        }
    }

    public static void tick() {
        if (num == 2) {
            syncInv();
        }
        addInv();

        if (LitematicaMixinMod.CLOSE_ALL_MODE.getKeybind().isPressed()) {
            LitematicaMixinMod.EXCAVATE.setBooleanValue(false);
            LitematicaMixinMod.FLUID.setBooleanValue(false);
            LitematicaMixinMod.PRINT_SWITCH.setBooleanValue(false);
            LitematicaMixinMod.PRINTER_MODE.setOptionListValue(State.PrintModeType.PRINTER);
            client.inGameHud.setOverlayMessage(Text.of("已关闭全部模式"), false);
        }
        OpenInventoryPacket.tick();
    }

    public static void switchPlayerInvToHotbarAir(int slot) {
        if (client.player == null) return;
        ClientPlayerEntity player = client.player;
        ScreenHandler sc = player.currentScreenHandler;
        DefaultedList<Slot> slots = sc.slots;
        int i = sc.equals(player.playerScreenHandler) ? 9 : 0;
        for (; i < slots.size(); i++) {
            if (slots.get(i).getStack().isEmpty() && slots.get(i).inventory instanceof PlayerInventory) {
                fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, i, slot);
                return;
            }
        }
    }

    public static boolean canInteracted(Vec3d d,double range){
        return client.player != null &&
                d != null &&
                client.player.getEyePos().squaredDistanceTo(d) < range * range;
    }

    public static boolean canInteracted(BlockPos blockPos) {
        return blockPos != null && canInteracted(Vec3d.ofCenter(blockPos), LitematicaMixinMod.COMPULSION_RANGE.getIntegerValue());
    }

    public static void exitGameReSet(){
        SwitchItem.reSet();
        isRemote = false;
        clientTry = false;
        remoteTime = 0;
    }

    public static int getEnchantmentLevel(ItemStack itemStack,
                                          //#if MC > 12006
                                          RegistryKey<Enchantment> enchantment
                                          //#else
                                          //$$ Enchantment enchantment
                                          //#endif
    ){
        //#if MC > 12006
        ItemEnchantmentsComponent enchantments = itemStack.getEnchantments();

        if (enchantments.equals(ItemEnchantmentsComponent.DEFAULT)) return -1;
        Set<RegistryEntry<Enchantment>> enchantmentsEnchantments = enchantments.getEnchantments();
        for (RegistryEntry<Enchantment> entry : enchantmentsEnchantments) {
            if (entry.matchesKey(enchantment)) {
                return enchantments.getLevel(entry);
            }
        }
        return -1;
        //#else
        //$$ return EnchantmentHelper.getLevel(enchantment,itemStack);
        //#endif
    }

    public static int getSequence() {
        return sequence++;
    }

    /**
     * 将指定槽位的物品设置为玩家当前手持物品。
     * <p>
     * 如果目标槽位是快捷栏槽位，则直接选中该槽位；
     * 否则尝试寻找一个合适的快捷栏槽位来放置该物品：
     * 1. 若 sourceSlot 为 -1 或无效，则优先使用空的可拾取槽位；
     * 2. 若没有空槽，则根据配置选择一个目标槽位；
     * 3. 在创造模式下通过交换方式将物品放入快捷栏；
     * 4. 在生存模式下通过点击槽位交换的方式进行操作。
     *
     * @param sourceSlot 源物品所在的槽位索引（若为 -1 表示未指定）
     * @param stack      要设置到手中的物品堆栈
     * @return 是否成功设置了手持物品
     */
    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack) {
        PlayerEntity player = client.player;
        PlayerInventory inventory = player.getInventory();
        var usePacket = LitematicaMixinMod.PLACE_USE_PACKET.getBooleanValue();

        // 如果源槽位是有效的快捷栏索引，直接切换选中该槽位
        if (PlayerInventory.isValidHotbarIndex(sourceSlot)) {
            selectHotbarSlot(sourceSlot, inventory, usePacket);
            return true;
        } else {
            int hotbarSlot = sourceSlot;

            // 如果源槽位无效，尝试获取一个空的可用于拾取的快捷栏槽位
            if (sourceSlot == -1 || !PlayerInventory.isValidHotbarIndex(sourceSlot)) {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            // 如果仍然没有找到合适槽位，则根据规则获取一个目标槽位
            if (hotbarSlot == -1) {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            // 如果找到了一个合适的快捷栏槽位
            if (hotbarSlot != -1) {
                // 先选中槽位
                selectHotbarSlot(hotbarSlot, inventory, usePacket);

                // 如果是创造模式，直接交换物品到快捷栏
                if (player.isCreative()) {
                    //#if MC <= 12101
                    //$$player.getInventory().addPickBlock(stack.copy());
                    //#else
                    player.getInventory().swapStackWithHotbar(stack.copy());
                    //#endif
                    if (usePacket)
                        client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + player.getInventory().selectedSlot, stack));
                    else
                        client.interactionManager.clickCreativeStack(stack, 36 + player.getInventory().selectedSlot);

                } else {
                    // 生存模式处理逻辑：查找物品所在槽位，并执行交换操作
                    int slot1 = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.playerScreenHandler, stack.copy(), true);
                    if (slot1 != -1) {
                        // 使用数据包或普通点击方式交换槽位中的物品
                        if (usePacket) {
                            isSwitching = !isSwitching;
                            DefaultedList<Slot> slots = player.currentScreenHandler.slots;
                            int totalSlots = slots.size();
                            List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
                            for (Slot slotItem : slots) {
                                copies.add(slotItem.getStack().copy());
                            }

                            Int2ObjectMap<
                                    //#if MC >= 12105
                                    //$$ ItemStackHash
                                    //#else
                                    ItemStack
                                    //#endif
                                    > snapshot = new Int2ObjectOpenHashMap<>();
                            for (int j = 0; j < totalSlots; j++) {
                                ItemStack original = copies.get(j);
                                ItemStack current = slots.get(j).getStack();
                                if (!ItemStack.areEqual(original, current)) {
                                    snapshot.put(j,
                                            //#if MC >=12105
                                            //$$ ItemStackHash.fromItemStack(current, client.getNetworkHandler().method_68823())
                                            //#else
                                            current.copy()
                                            //#endif
                                    );
                                }
                            }

                            //#if MC >= 12105
                            //$$ItemStackHash itemStackHash = ItemStackHash.fromItemStack(player.currentScreenHandler.getCursorStack(), client.getNetworkHandler().method_68823());
                            //#endif
                            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                    player.playerScreenHandler.syncId,
                                    player.currentScreenHandler.getRevision(),
                                    //#if MC >= 12105
                                    //$$ Shorts.checkedCast((long)slot1),
                                    //$$ SignedBytes.checkedCast((long)hotbarSlot),
                                    //#else
                                    slot1,
                                    hotbarSlot,
                                    //#endif
                                    SlotActionType.SWAP,
                                    //#if MC >= 12105
                                    //$$ snapshot,
                                    //$$ itemStackHash
                                    //#else
                                    stack.copy(),
                                    snapshot
                                    //#endif
                            ));
                            client.player.currentScreenHandler.onSlotClick(slot1, hotbarSlot, SlotActionType.SWAP, player);
                            WorldUtils.setEasyPlaceLastPickBlockTime();
                            return !isSwitching;
                        } else {
                            client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, hotbarSlot, SlotActionType.SWAP, player);
                            return true;
                        }
                    }
                }

                WorldUtils.setEasyPlaceLastPickBlockTime();
                return true;
            } else {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
                return false;
            }
        }
    }

    private static void selectHotbarSlot(int slot, PlayerInventory inventory, boolean usePacket) {
        if (usePacket) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        //#if MC > 12101
        inventory.setSelectedSlot(slot);
        //#else
        //$$ inventory.selectedSlot = slot;
        //#endif
    }



    public static void actionBar(String message){
        //#if MC > 11802
        MutableText translatable = Text.translatable(message);
        //#else
        //$$ TranslatableText translatable = new TranslatableText(message);
        //#endif
        client.inGameHud.setOverlayMessage(translatable,false);
    }

    public static LinkedList<BlockPos> siftBlock(String blockName) {
        LinkedList<BlockPos> blocks = new LinkedList<>();
        AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
        List<Box> boxes;
        if (i == null) return blocks;
        boxes = i.getAllSubRegionBoxes();
        for (Box box : boxes) {
            MyBox myBox = new MyBox(box);
            for (BlockPos pos : myBox) {
                BlockState state = null;
                if (Printer.client.world != null) {
                    state = Printer.client.world.getBlockState(pos);
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
