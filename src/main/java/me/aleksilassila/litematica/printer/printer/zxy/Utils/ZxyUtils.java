package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket.openIng;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static net.minecraft.block.ShulkerBoxBlock.FACING;

public class ZxyUtils {
    @NotNull
    public static MinecraftClient client = MinecraftClient.getInstance();
    public static LinkedList<BlockPos> invBlockList = new LinkedList<>();
    public static boolean printerMemoryAdding = false;

    public static void startAddPrinterInventory(){
        getReadyColor();
        if (LitematicaMixinMod.INVENTORY.getBooleanValue() && !printerMemoryAdding) {
            printerMemoryAdding = true;
            if (MemoryUtils.PRINTER_MEMORY == null) MemoryUtils.createPrinterMemory();
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
                client.inGameHud.setOverlayMessage(Text.literal("打印机库存添加完成"), false);
                return;
            }
            client.inGameHud.setOverlayMessage(Text.literal("添加库存中"), false);
            for (BlockPos pos : invBlockList) {
                if (client.world != null) {
//                    MemoryUtils.setLatestPos(pos);
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
    static Color4f color4f;
    static List<BlockPos> highlightPosList = new LinkedList<>();
    private static void getReadyColor(){
        color4f = LitematicaMixinMod.SYNC_INVENTORY_COLOR.getColor();
        HighlightBlockRenderer.addHighlightMap(color4f);
        highlightPosList = HighlightBlockRenderer.getPosList(color4f);
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
                    if (((BlockWithEntity) blockState.getBlock()).createScreenHandlerFactory(blockState, client.world, pos) == null ||
                            (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                    !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                                    entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED)) {
                        client.inGameHud.setOverlayMessage(Text.literal("容器无法打开"), false);
                    }
                } catch (Exception e) {
                    client.inGameHud.setOverlayMessage(Text.literal("这不是容器 无法同步"), false);
                    return;
                }
            }
            String blockName = Registries.BLOCK.getId(block).toString();
            if (Printer.getPrinter() != null) {
                syncPosList.addAll(Printer.getPrinter().siftBlock(blockName));
                highlightPosList.addAll(syncPosList);
            }
            if (!syncPosList.isEmpty()) {
                if (client.player == null) return;
                client.player.closeHandledScreen();
                if (!openInv(pos,false))return;
                closeScreen++;
                num = 1;
            }
        } else {
            highlightPosList.removeAll(syncPosList);
            syncPosList = new LinkedList<>();
            if (client.player != null) client.player.closeScreen();
            num = 0;
            client.inGameHud.setOverlayMessage(Text.of("已取消同步"),false);
        }
    }
    public static boolean openInv(BlockPos pos,boolean ignoreThePrompt){
        if(LitematicaMixinMod.INVENTORY.getBooleanValue()) {
            OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
            return true;
        } else {
            if (client.player != null && client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 25D) {
                if(!ignoreThePrompt) client.inGameHud.setOverlayMessage(Text.literal("距离过远无法打开容器"), false);
                return false;
            }
            if (client.interactionManager != null){
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN,pos,false));
                return true;
            } else return false;
        }
    }

    public static void syncInv() {
        switch (num) {
            case 1 -> {
                //按下热键后记录看向的容器 开始同步容器 只会触发一次
                targetBlockInv = new ArrayList<>();
                if (client.player != null && (!LitematicaMixinMod.INVENTORY.getBooleanValue() || openIng) && !client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) {
                    for (int i = 0; i < client.player.currentScreenHandler.slots.get(0).inventory.size(); i++) {
                        targetBlockInv.add(client.player.currentScreenHandler.slots.get(i).getStack().copy());
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
                client.inGameHud.setOverlayMessage(Text.literal("剩余 " + syncPosList.size() + " 个容器. 再次按下快捷键取消同步"), false);
                if (client.player != null && !client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) return;
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
                    client.inGameHud.setOverlayMessage(Text.literal("同步完成"), false);
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
//                            System.out.println(item2);
                    int currNum = item1.getCount();
                    int tarNum = item2.getCount();
                    boolean same = new ItemType(item1).equals(new ItemType(item2.copy()));
//                            System.out.println(currNum);
//                            System.out.println(tarNum);
                    //不和背包交互
                    if (same) {
                        //有多
                        while (currNum > tarNum) {
                            client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.THROW, client.player);
                            currNum = item1.getCount();
                        }
                    } else {
                        //不同直接扔出
                        client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.THROW, client.player);
                        times++;
                    }
                    //背包交互
                    for (int i1 = size; i1 < sc.slots.size(); i1++) {
                        ItemStack stack = sc.slots.get(i1).getStack();
                        currNum = sc.slots.get(i).getStack().getCount();
                        boolean same2 = new ItemType(item2).equals(new ItemType(stack));
                        if (same2 && !stack.isEmpty()) {
//                                    System.out.println(i+"  tarNum\t"+tarNum);
//                                    System.out.println(i+"  currNum\t"+currNum);
                            int i2 = stack.getCount();
                            client.interactionManager.clickSlot(sc.syncId, i1, 0, SlotActionType.PICKUP, client.player);
                            for (; currNum < tarNum && i2 > 0; i2--) {
                                client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.PICKUP, client.player);
                                currNum = sc.slots.get(i).getStack().getCount();
                            }
                            client.interactionManager.clickSlot(sc.syncId, i1, 0, SlotActionType.PICKUP, client.player);
                        }
                        if (currNum < tarNum) times++;
                    }
                    if (times == 0) {
                        syncPosList.remove(blockPos);
                        highlightPosList.remove(blockPos);
                        blockPos = null;
                    }
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
            LitematicaMixinMod.PRINT_MODE.setBooleanValue(false);
            client.inGameHud.setOverlayMessage(Text.literal("已关闭全部模式"), false);
        }
//        for (BlockPos pos : syncPosList) {
//            RenderUtils.FOUND_ITEM_POSITIONS.put(pos, new PositionData(pos, client.world.getTime(), VoxelShapes.fullCube(), 10, 10, 6, null));
//        }
        test();
    }

    static ItemStack itemStack;

    public static void test() {
        ClientPlayerEntity player = client.player;
        BlockPos blockPos1 = player.getBlockPos().up(-1);
        BlockPos blockPos2 = ((BlockHitResult) client.crosshairTarget).getBlockPos();
//        HighlightBlockRenderer.test(blockPos1);
//        HighlightBlockRenderer.renderHighlightedBlock(client.world.getBlockState(blockPos1),blockPos1,client.world,null,null);
        if (LitematicaMixinMod.TEST.getKeybind().isPressed()) {
//            QuickShulkerUtils.test();
            if (itemStack == null) itemStack = client.player.getInventory().getMainHandStack();
            if (!InventoryUtils.areStacksEqual(client.player.getInventory().getMainHandStack(), itemStack)) {
                itemStack = client.player.getInventory().getMainHandStack();
                System.out.println("=======");
            }
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
        return optionListValue != State.ListType.SPHERE ||
                (client.player != null &&
                        d != null &&
                        client.player.getEyePos().squaredDistanceTo(d) < range * range);
    }

    public static boolean canInteracted(BlockPos blockPos, double range) {
        return blockPos != null && canInteracted(Vec3d.ofCenter(blockPos), range);
    }

    public static boolean bedrockCanInteracted(BlockPos blockPos, double range) {
        return client.player != null && client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) < range * range;
    }

    public static int getPrinterRange() {
        return LitematicaMixinMod.PRINTING_RANGE.getIntegerValue();
    }

    public static int frameGenerationTime = getMonitorRefreshRate();

    public static int getMonitorRefreshRate() {
        int refreshRate = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()).refreshRate();
        return 1000 / refreshRate;
//        System.out.println("The monitor refresh rate is " + refreshRate);
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
