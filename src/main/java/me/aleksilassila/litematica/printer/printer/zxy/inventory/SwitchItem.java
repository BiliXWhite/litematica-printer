package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;

public class SwitchItem {
//    public static boolean switchIng = false;
    public static ItemStack reSwitchItem = null;
    public static Map<ItemStack,ItemStatistics> itemStacks = new HashMap<>();
    public static void removeItem(ItemStack itemStack){
        itemStacks.remove(itemStack);
//        itemStacks.entrySet().removeIf(var -> var.getKey().getItem().equals(itemStack.getItem()));
    }
    public static void syncUseTime(ItemStack itemStack){
        ItemStatistics itemStatistics = itemStacks.get(itemStack);
        if(itemStatistics != null) itemStatistics.syncUseTime();
    }
    public static void newItem(ItemStack itemStack, BlockPos pos, RegistryKey<World> key, int slot, int shulkerBox){
        itemStacks.put(itemStack,new ItemStatistics(key,pos,slot,shulkerBox));
    }
    public static void openInv(ItemStack itemStack){
        if(!client.player.currentScreenHandler.equals(client.player.playerScreenHandler) || Statistics.closeScreen > 0){
            return;
        }
        ScreenHandler sc1 = client.player.currentScreenHandler;
        if (sc1.slots.stream().skip(9).limit(sc1.slots.size()-10)
                .noneMatch(slot -> InventoryUtils.areStacksEqual(slot.getStack(),reSwitchItem))) {
            itemStacks.remove(reSwitchItem);
            reSwitchItem = null;
            return;
        }
        ItemStatistics itemStatistics = itemStacks.get(itemStack);
        if(itemStatistics != null){
            if (itemStatistics.key != null && OpenInventoryPacket.key == null) {
                OpenInventoryPacket.sendOpenInventory(itemStatistics.pos,itemStatistics.key);
                Statistics.closeScreen++;
            }else {
                ScreenHandler sc = client.player.currentScreenHandler;
                //因使用快捷濳影盒打开物品列表后无法更新，所以读取的盒子物品列表没意义。
//                DefaultedList<ItemStack> storedItems = InventoryUtils.getStoredItems(sc.slots.get(itemStatistics.shulkerBoxSlot).getStack(), -1);
//                if(storedItems.get(itemStatistics.slot).isEmpty()){
                    try {
                        Class quickShulker = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
                        Method checkAndSend = quickShulker.getDeclaredMethod("CheckAndSend",ItemStack.class,int.class);
                        checkAndSend.invoke(checkAndSend,sc.slots.get(itemStatistics.shulkerBoxSlot).getStack(),itemStatistics.shulkerBoxSlot);
                        Statistics.closeScreen++;
                    } catch (Exception ignored){
                        removeItem(reSwitchItem);
                        reSwitchItem = null;
                    }
//                }
//                for (int i = 9; i < sc.slots.size() && itemStatistics.shulkerBoxSlot != -1; i++) {
//                    ItemStack stack = sc.slots.get(i).getStack();
//                    if (InventoryUtils.getStoredItems(stack,-1).stream().anyMatch(stack1 -> stack1.isEmpty() ||
//                            (InventoryUtils.areStacksEqual(stack1,reSwitchItem) && stack1.getCount() + reSwitchItem.getCount() <= stack1.getMaxCount()))
//                    ) {
//                        try {
//                            Class quickShulker = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
//                            Method checkAndSend = quickShulker.getDeclaredMethod("CheckAndSend",ItemStack.class,int.class);
//                            checkAndSend.invoke(checkAndSend,sc.slots.get(itemStatistics.shulkerBoxSlot).getStack(),i);
//                            Statistics.closeScreen++;
//                            return;
//                        } catch (Exception ignored){}
//                    }
//                }

            }
        }else {
            removeItem(reSwitchItem);
            reSwitchItem = null;
        }
    }
    public static void checkItems(){
        final long[] min = {System.currentTimeMillis()};
        AtomicReference<ItemStack> key = new AtomicReference<>();
        itemStacks.keySet().forEach(k ->{
            long useTime = itemStacks.get(k).useTime;
            if(useTime < min[0]){
                min[0] = useTime;
                key.set(k);
            }
        });
        ItemStack itemStack = key.get();
        if(itemStack != null) {
            reSwitchItem = itemStack;
            openInv(itemStack);
        }else client.inGameHud.setOverlayMessage(Text.of("背包已满，请先清理"),false);
    }
    public static void reSwitchItem(){
        if(client.player == null || reSwitchItem == null) return;
        ClientPlayerEntity player = client.player;
        ScreenHandler sc = player.currentScreenHandler;
        if (sc.equals(player.playerScreenHandler)) return;

        List<Integer> sameItem = new ArrayList<>();
        for (int i = 0; i < sc.slots.size(); i++) {
            Slot slot = sc.slots.get(i);
            if(!(slot.inventory instanceof PlayerInventory) &&
                    InventoryUtils.areStacksEqual(reSwitchItem,slot.getStack()) &&
                    slot.getStack().getCount() < slot.getStack().getMaxCount()
            ) sameItem.add(i);
            if(slot.inventory instanceof PlayerInventory && client.interactionManager != null && InventoryUtils.areStacksEqual(slot.getStack(),reSwitchItem)){
                int slot1 = itemStacks.get(reSwitchItem).slot;
                boolean reInv = false;
                //检查记录的槽位是否有物品
                ItemStack stack = sc.slots.get(slot1).getStack();
                if(sc.slots.get(slot1).getStack().isEmpty()){
                    client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(sc.syncId, slot1, 0, SlotActionType.PICKUP, client.player);
                    reInv = true;
                } else {
                    int count = reSwitchItem.getCount();
                    client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
                    for (Integer integer : sameItem) {
                        int count1 = sc.slots.get(integer).getStack().getCount();
                        int maxCount = sc.slots.get(integer).getStack().getMaxCount();
                        int i1 = maxCount - count1;
                        count -= i1;
                        client.interactionManager.clickSlot(sc.syncId, integer, 0, SlotActionType.PICKUP, client.player);
                        if (count<=0) reInv = true;
                    }
                }
                removeItem(reSwitchItem);
                reSwitchItem = null;
                player.closeHandledScreen();
                if(!reInv) {
                    client.inGameHud.setOverlayMessage(Text.of("复原库存物品失败"),false);
                }
                client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
                return;
            }
        }
    }
    public static void reSet(){
        reSwitchItem = null;
        itemStacks = new HashMap<>();
    }
    public static class ItemStatistics {
        public RegistryKey<World> key;
        public BlockPos pos;
        public int slot;
        public int shulkerBoxSlot;
        public long useTime = System.currentTimeMillis();
        public ItemStatistics(RegistryKey<World> key, BlockPos pos, int slot, int shulkerBox) {
            this.key = key;
            this.pos = pos;
            this.slot = slot;
            this.shulkerBoxSlot = shulkerBox;
        }
        public void syncUseTime(){
            this.useTime = System.currentTimeMillis();
        }
    }
}