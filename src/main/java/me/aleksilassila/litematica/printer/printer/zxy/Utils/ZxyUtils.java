package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.Color4f;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;

import me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController;
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
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
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
import org.lwjgl.glfw.GLFW;

import java.util.*;
//#if MC >= 12001
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#else
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//#endif
//#if MC > 12006
//$$ import net.minecraft.registry.RegistryKey;
//$$ import net.minecraft.component.type.ItemEnchantmentsComponent;
//$$ import net.minecraft.registry.entry.RegistryEntry;
//#endif
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.SYNC_INVENTORY_CHECK;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.SYNC_INVENTORY_COLOR;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static net.minecraft.block.ShulkerBoxBlock.FACING;

public class ZxyUtils {
    //旧版箱子追踪
    public static boolean qw = false;
    public static int currWorldId = 0;

    @NotNull
    public static MinecraftClient client = MinecraftClient.getInstance();
    public static LinkedList<BlockPos> invBlockList = new LinkedList<>();
    public static boolean printerMemoryAdding = false;
    public static boolean syncPrinterInventory = false;
    public static String syncInventoryId = "syncInventory";

    public static void startAddPrinterInventory(){
        getReadyColor();
        if (LitematicaMixinMod.INVENTORY.getBooleanValue() && !printerMemoryAdding) {
            printerMemoryAdding = true;
            //#if MC >= 12001
            //$$ if (MemoryUtils.PRINTER_MEMORY == null) MemoryUtils.createPrinterMemory();
            //#endif

            for (String string : LitematicaMixinMod.INVENTORY_LIST.getStrings()) {
                if (Printer.getPrinter() != null) {
                    invBlockList.addAll(Printer.getPrinter().siftBlock(string));
                }
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
                    MemoryUtils.setLatestPos(pos);
                    //#endif
                    closeScreen++;
                    OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
//                    ((IClientPlayerInteractionManager) client.interactionManager)
//                            .rightClickBlock(pos,Direction.UP ,new Vec3d(pos.getX(), pos.getY(), pos.getZ()) );
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
    static List<BlockPos> highlightPosList = new LinkedList<>();
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
                try {
                    if ((!InventoryUtils.isInventory(client.world,pos) && blockState.createScreenHandlerFactory(client.world,pos) == null)||
                            (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                    //#if MC > 12004
                                    //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F).offset(pos).contract(1.0E-6)) &&
                                    //#else
                                    !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                                    //#endif
                                    entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED)) {
                        client.inGameHud.setOverlayMessage(Text.of("容器无法打开"), false);
                    }
                } catch (Exception e) {
                    client.inGameHud.setOverlayMessage(Text.of("这不是容器 无法同步"), false);
                    return;
                }
            }
            String blockName = Registry.BLOCK.getId(block).toString();
            if (Printer.getPrinter() != null) {
                syncPosList.addAll(Printer.getPrinter().siftBlock(blockName));
            }
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
            highlightPosList.removeAll(syncPosList);
            syncPosList = new LinkedList<>();
            if (client.player != null) client.player.closeScreen();
            num = 0;
            client.inGameHud.setOverlayMessage(Text.of("已取消同步"), false);
        }
    }
    public static boolean openInv(BlockPos pos,boolean ignoreThePrompt){
        if(LitematicaMixinMod.INVENTORY.getBooleanValue() && OpenInventoryPacket.key == null) {
            OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
            return true;
        } else {
            if (client.player != null && !canInteracted(5,Vec3d.ofCenter(pos))) {
                if(!ignoreThePrompt) client.inGameHud.setOverlayMessage(Text.of("距离过远无法打开容器"), false);
                return false;
            }
            if (client.interactionManager != null){
                //#if MC < 11904
                client.interactionManager.interactBlock(client.player, client.world, Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN,pos,false));
                //#else
                //$$ client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN,pos,false));
                //#endif
                return true;
            } else return false;
        }
    }
    public static void itemsCount(Map<ItemStack,Integer> itemsCount , ItemStack itemStack){
        // 判断是否存在可合并的键
        Optional<Map.Entry<ItemStack, Integer>> entry = itemsCount.entrySet().stream()
                .filter(e -> ItemStack.canCombine(e.getKey(), itemStack))
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
                if (client.player != null && (!LitematicaMixinMod.INVENTORY.getBooleanValue() || openIng) && !client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) {
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
//                if(targetItemsCount.keySet().stream()
//                        .noneMatch(itemStack -> playerItemsCount.keySet().stream()
//                                .anyMatch(itemStack1 -> ItemStack.canCombine(itemStack,itemStack1)))) return;
                if (SYNC_INVENTORY_CHECK.getBooleanValue() && !targetItemsCount.entrySet().stream()
                        .allMatch(target -> playerItemsCount.entrySet().stream()
                                .anyMatch(player ->
                                        ItemStack.canCombine(player.getKey(), target.getKey()) && target.getValue() <= player.getValue()))) return;

                if ((!LitematicaMixinMod.INVENTORY.getBooleanValue() || !openIng) && OpenInventoryPacket.key == null) {
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
                    boolean same = ItemStack.canCombine(item1,item2.copy()) && !item1.isEmpty();
                    if(ItemStack.canCombine(item1,item2) && currNum == tarNum) continue;
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
                        boolean same2 = thereAreItems = ItemStack.canCombine(item2,stack);
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
//        if (LitematicaMixinMod.REMOVE_PRINT_INVENTORY.getKeybind().isPressed()) {
////            MemoryDatabase database = MemoryDatabase.getCurrent();
////            if (database != null) {
////                for (Identifier dimension : database.getDimensions()) {
////                    database.clearDimension(dimension);
////                }
////            }
//        }
        if (LitematicaMixinMod.CLOSE_ALL_MODE.getKeybind().isPressed()) {
            LitematicaMixinMod.BEDROCK_SWITCH.setBooleanValue(false);
            LitematicaMixinMod.EXCAVATE.setBooleanValue(false);
            LitematicaMixinMod.FLUID.setBooleanValue(false);
            LitematicaMixinMod.PRINT_SWITCH.setBooleanValue(false);
            LitematicaMixinMod.PRINTER_MODE.setOptionListValue(State.PrintModeType.PRINTER);
            client.inGameHud.setOverlayMessage(Text.of("已关闭全部模式"), false);
        }
        OpenInventoryPacket.tick();
//        for (BlockPos pos : syncPosList) {
//            RenderUtils.FOUND_ITEM_POSITIONS.put(pos, new PositionData(pos, client.world.getTime(), VoxelShapes.fullCube(), 10, 10, 6, null));
//        }
        test();
    }

    static ItemStack itemStack;
    public static void test() {
        if (LitematicaMixinMod.TEST.getKeybind().isPressed()) {
//            QuickShulkerUtils.test();
//            if (itemStack == null) itemStack = client.player.getInventory().getMainHandStack();
//            if (!InventoryUtils.areStacksEqual(client.player.getInventory().getMainHandStack(), itemStack)) {
//                itemStack = client.player.getInventory().getMainHandStack();
//                System.out.println("=======");
//            }
//            OpenInventoryPacket.sendOpenInventory(DataManager.getSelectionManager().getCurrentSelection().getSubRegionBox(DataManager.getSimpleArea().getName()).getPos1(),MinecraftClient.getInstance().world.getRegistryKey());
        }
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

    public static boolean canInteracted(Vec3d d, double range) {
        IConfigOptionListEntry optionListValue = LitematicaMixinMod.RANGE_MODE.getOptionListValue();
        return optionListValue != State.ListType.SPHERE || canInteracted(range,d);
    }
    public static boolean canInteracted(double range,Vec3d d){
        return client.player != null &&
                d != null &&
                client.player.getEyePos().squaredDistanceTo(d) < range * range;
    }

    public static boolean canInteracted(BlockPos blockPos) {
        return blockPos != null && canInteracted(Vec3d.ofCenter(blockPos),getRage());
    }

    public static boolean bedrockCanInteracted(BlockPos blockPos,double range) {
        return client.player != null && client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) < range * range;
    }
    public static int getRage(){
        return Math.max(getPrinterRange(),getCompulsionRange());
    }
    public static int getPrinterRange() {
        return LitematicaMixinMod.PRINTING_RANGE.getIntegerValue();
    }
    public static int getCompulsionRange(){
        return LitematicaMixinMod.COMPULSION_RANGE.getIntegerValue();
    }

    public static int frameGenerationTime = getMonitorRefreshRate();

    public static int getMonitorRefreshRate() {
        int refreshRate = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()).refreshRate();
        return 1000 / refreshRate;
//        System.out.println("The monitor refresh rate is " + refreshRate);
    }
    public static void reSet(){
        SwitchItem.reSet();
        Verify.verify = null;
        BreakingFlowController.poslist = new ArrayList<>();
        isRemote = false;
        clientTry = false;
        remoteTime = 0;
    }

    public static void useBlock(Vec3d vec3d,Direction direction,BlockPos pos,boolean insideBlock){
        //#if MC < 11904
        client.interactionManager.interactBlock(client.player, client.world, Hand.MAIN_HAND,new BlockHitResult(vec3d, direction,pos,insideBlock));
        //#else
        //$$ client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,new BlockHitResult(vec3d, direction,pos,insideBlock));
        //#endif
    }

    public static int getEnchantmentLevel(ItemStack itemStack,
                                          //#if MC > 12006
                                          //$$ RegistryKey<Enchantment> enchantment
                                          //#else
                                          Enchantment enchantment
                                          //#endif
    ){
        //#if MC > 12006
        //$$ ItemEnchantmentsComponent enchantments = itemStack.getEnchantments();
        //$$
        //$$ if (enchantments.equals(ItemEnchantmentsComponent.DEFAULT)) return -1;
        //$$ Set<RegistryEntry<Enchantment>> enchantmentsEnchantments = enchantments.getEnchantments();
        //$$ for (RegistryEntry<Enchantment> entry : enchantmentsEnchantments) {
        //$$     if (entry.matchesKey(enchantment)) {
        //$$         return enchantments.getLevel(entry);
        //$$     }
        //$$ }
        //$$ return -1;
        //#else
        return EnchantmentHelper.getLevel(enchantment,itemStack);
        //#endif
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
}
