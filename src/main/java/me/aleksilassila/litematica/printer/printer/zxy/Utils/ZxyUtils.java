package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.util.ItemType;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedList;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket.openIng;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static net.minecraft.block.ShulkerBoxBlock.FACING;

public class ZxyUtils {
    public static MinecraftClient client = MinecraftClient.getInstance();
    public static LinkedList<BlockPos> invBlockList = new LinkedList<>();
    public static boolean printerMemoryAdding = false;
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
                break;
            }
        } else if (LitematicaMixinMod.PRINTER_INVENTORY.getKeybind().isKeybindHeld() && LitematicaMixinMod.INVENTORY.getBooleanValue() && !printerMemoryAdding) {
            printerMemoryAdding = true;
            for (String string : LitematicaMixinMod.INVENTORY_LIST.getStrings()) {
                if (Printer.getPrinter() != null) {
                    invBlockList.addAll(Printer.getPrinter().siftBlock(string));
                }
            }

        }
    }

    public static LinkedList<BlockPos> syncPosList = new LinkedList<>();
    public static ArrayList<ItemStack> targetBlockInv;
    public static int num = 1;
    static BlockPos blockPos = null;

    public static void syncInv() {
        switch (num) {
            case 1 -> {
                if (LitematicaMixinMod.SYNC_INVENTORY.getKeybind().isKeybindHeld() && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
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
                                client.inGameHud.setOverlayMessage(Text.literal("八嘎，目标容器无法打开"), false);
                            }
                        } catch (Exception e) {
                            client.inGameHud.setOverlayMessage(Text.literal("八嘎，这不是容器 无法同步"), false);
                            return;
                        }
                    }
                    String blockName = Registries.BLOCK.getId(block).toString();
                    if (Printer.getPrinter() != null) {
                        syncPosList.addAll(Printer.getPrinter().siftBlock(blockName));
                    }
                    if (syncPosList.size() != 0) {
                        OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
                        closeScreen++;
                        num = 2;
                    }
                }
            }
            case 2 -> {
                targetBlockInv = new ArrayList<>();
                closeScreen++;
                if (client.player != null && openIng && !client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) {
                    for (int i = 0; i < client.player.currentScreenHandler.slots.get(0).inventory.size(); i++) {
//                        System.out.println(i+" itemStack:  "+client.player.currentScreenHandler.slots.get(i).getStack());
//                        System.out.println(i+" num:  "+client.player.currentScreenHandler.slots.get(i).getStack().getCount());
                        targetBlockInv.add(client.player.currentScreenHandler.slots.get(i).getStack().copy());
                    }
                    //上面如果不使用copy()在关闭容器后会使第一个元素号变该物品成总数 非常有趣...
//                    System.out.println("???1 "+targetBlockInv.get(0).getCount());
                    client.player.closeHandledScreen();
//                    System.out.println("!!!1 "+targetBlockInv.get(0).getCount());
                    num = 3;
                }
            }
            case 3 -> {
                if (!openIng && OpenInventoryPacket.key == null) {
                    for (BlockPos pos : syncPosList) {
                        closeScreen++;
                        OpenInventoryPacket.sendOpenInventory(pos, client.world.getRegistryKey());
                        blockPos = pos;
                        break;
                    }
                }else {
                    ScreenHandler sc = client.player.currentScreenHandler;
                    if (!sc.equals(client.player.playerScreenHandler)) {
                        int size = sc.slots.get(0).inventory.size();

                        //右键单击
//                        client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.PICKUP, client.player);
                        //左键单击
//                        client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);

                        //丢弃一个
//                        client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.THROW, client.player);
                        //丢弃全部
//                        client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.THROW, client.player);;
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
                            }else{
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
                                if(currNum < tarNum) times++;
                            }
                            if(times == 0){
                                syncPosList.remove(blockPos);
                                blockPos = null;
                            }
                        }
                        client.player.closeHandledScreen();
                    }
                    client.inGameHud.setOverlayMessage(Text.literal("剩余 "+syncPosList.size()+" 个容器. 再次按下快捷键取消同步"), false);
                    if(LitematicaMixinMod.SYNC_INVENTORY.getKeybind().isKeybindHeld()){
                        syncPosList = new LinkedList<>();
                        client.player.closeHandledScreen();
                        num = 1;
                        return;
                    }
                    if (syncPosList.isEmpty()) {
                        num = 1;
                    }
                }
            }
        }
    }

    public static void tick() {
        if (LitematicaMixinMod.REVISION_PRINT.getKeybind().isKeybindHeld()) {
//            MemoryDatabase database = MemoryDatabase.getCurrent();
//            if (database != null) {
//                for (Identifier dimension : database.getDimensions()) {
//                    database.clearDimension(dimension);
//                }
//            }
            MemoryUtils.deletePrinterMemory();
            client.inGameHud.setOverlayMessage(Text.literal("打印机库存已清空"), false);
        }
        for (BlockPos pos : syncPosList) {
//            RenderUtils.FOUND_ITEM_POSITIONS.put(pos, new PositionData(pos, client.world.getTime(), VoxelShapes.fullCube(), 10, 10, 6, null));
        }
        test();
    }
    public static void test(){
        if (LitematicaMixinMod.TEST.getKeybind().isKeybindHeld()) {
            OpenInventoryPacket.sendOpenInventory(DataManager.getSelectionManager().getCurrentSelection().getSubRegionBox(DataManager.getSimpleArea().getName()).getPos1(),MinecraftClient.getInstance().world.getRegistryKey());
        }
    }
}
