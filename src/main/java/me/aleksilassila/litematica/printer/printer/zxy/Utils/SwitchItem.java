package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;

public class SwitchItem {
    public static boolean switchIng = false;
    static ItemStack reSwitchItem = null;
    public static Map<ItemStack,ItemStatistics> itemStacks = new HashMap<>();
    public static void removeItem(ItemStack itemStack){
        itemStacks.remove(itemStack);
    }
    public static void syncUseTime(ItemStack itemStack){
        ItemStatistics itemStatistics = itemStacks.get(itemStack);
        if(itemStatistics !=null) itemStatistics.syncUseTime();
    }
    public static void newItem(ItemStack itemStack,BlockPos pos,Identifier key,int slot,ItemStack shulkerBox){
        itemStacks.put(itemStack,new ItemStatistics(key,pos,slot,shulkerBox));
    }
    public static void openInv(ItemStack itemStack){
        if(client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) return;
        ItemStatistics itemStatistics = itemStacks.get(itemStack);
        if(itemStatistics != null){
            if (itemStatistics.key != null) {
                OpenInventoryPacket.sendOpenInventory(itemStatistics.pos,RegistryKey.of(RegistryKeys.WORLD,itemStatistics.key));
            }else if (client.player != null) {
                ScreenHandler sc = client.player.currentScreenHandler;
                ItemStack shulkerBox = itemStatistics.shulkerBox;
                for (int i = 9; i < sc.slots.size(); i++) {
                    if (InventoryUtils.areStacksEqual(sc.slots.get(i).getStack(),shulkerBox)) {
                        try {
                            Class quickShulker = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
                            Method checkAndSend = quickShulker.getDeclaredMethod("CheckAndSend",ItemStack.class,int.class);
                            checkAndSend.invoke(checkAndSend,shulkerBox,i);
                        } catch (Exception ignored){}
                    }
                }
            }
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
        if(itemStack != null && client.world != null) {
            switchIng = true;
            reSwitchItem = itemStack;
            openInv(itemStack);
        }
    }
    public static void reSwitchItem(){
        if(client == null || client.player == null || reSwitchItem == null) return;
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
            if(slot.inventory instanceof PlayerInventory &&
                    InventoryUtils.areStacksEqual(reSwitchItem,slot.getStack()) &&
                    client.interactionManager != null
            ){
                int slot1 = itemStacks.get(reSwitchItem).slot;
                //检查记录的槽位是否有物品
                if(sc.slots.get(slot1).getStack().isEmpty()){
                    client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(sc.syncId, slot1, 0, SlotActionType.PICKUP, client.player);
                    switchIng = false;
                    reSwitchItem = null;
                    player.closeHandledScreen();
                } else if(!sameItem.isEmpty()){
                    int count = reSwitchItem.getCount();
                    client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
                    for (Integer integer : sameItem) {
                        int count1 = sc.slots.get(integer).getStack().getCount();
                        int maxCount = sc.slots.get(integer).getStack().getMaxCount();
                        int i1 = maxCount - count1;
                        count -= i1;
                        client.interactionManager.clickSlot(sc.syncId, integer, 0, SlotActionType.PICKUP, client.player);
                        if (count<=0){
                            switchIng = false;
                            reSwitchItem = null;
                            player.closeHandledScreen();
                            return;
                        }
                    }
                    client.inGameHud.setOverlayMessage(Text.of("复原物品位置失败"),false);
                    client.interactionManager.clickSlot(sc.syncId, i, 0, SlotActionType.PICKUP, client.player);
                }
            }
        }
    }
    public static void reSet(){
        switchIng = false;
        reSwitchItem = null;
        itemStacks = new HashMap<>();
    }
    public static class ItemStatistics {
        public Identifier key;
        public BlockPos pos;
        public int slot;
        public ItemStack shulkerBox;
        public long useTime = System.currentTimeMillis();
        public ItemStatistics(Identifier key, BlockPos pos, int slot, ItemStack shulkerBox) {
            this.key = key;
            this.pos = pos;
            this.slot = slot;
            this.shulkerBox = shulkerBox;
        }
        public void syncUseTime(){
            this.useTime = System.currentTimeMillis();
        }
    }
}