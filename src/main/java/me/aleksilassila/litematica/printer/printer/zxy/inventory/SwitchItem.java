package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.DebugUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.ShulkerUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.minecraft.client.MinecraftClient;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SwitchItem {
    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();
    public static ItemStack reSwitchItem = null;
    public static Map<ItemStack,ItemStatistics> itemStacks = new HashMap<>();
    public static void removeItem(ItemStack itemStack){
        itemStacks.remove(itemStack);
    }
    public static void syncUseTime(ItemStack itemStack){
        ItemStatistics itemStatistics = itemStacks.get(itemStack);
        if(itemStatistics != null) itemStatistics.syncUseTime();
    }
    public static void newItem(ItemStack itemStack, BlockPos pos, RegistryKey<World> key, int slot, int shulkerBox){
        if(shulkerBox != -1) itemStacks.put(itemStack,new ItemStatistics(key,pos,slot,shulkerBox));
    }
    public static void openInv(ItemStack itemStack) {
        DebugUtils.printChatMessage("Invoked openInv()");
        if(!client.player.currentScreenHandler.equals(client.player.playerScreenHandler) || Statistics.closeScreen > 0){
            return;
        }
        ScreenHandler sc = client.player.currentScreenHandler;
        if (sc.slots.stream().skip(9).limit(sc.slots.size() - 10)
                .noneMatch(slot -> InventoryUtils.areStacksEqual(slot.getStack(), reSwitchItem))) {
            itemStacks.remove(reSwitchItem);
            reSwitchItem = null;
            return;
        }
        ItemStatistics itemStatistics = itemStacks.get(itemStack);
        if (itemStatistics != null) {
            if (itemStatistics.key != null && OpenInventoryPacket.key == null) {
                OpenInventoryPacket.sendOpenInventory(itemStatistics.pos, itemStatistics.key);
            } else {
                ShulkerUtils.openShulker(itemStatistics.shulkerBoxSlot);
            }
            Statistics.closeScreen++;
        } else {
            removeItem(reSwitchItem);
            reSwitchItem = null;
        }
    }
    /**
     * 检查所有已记录的物品，找到最近一次使用的物品（useTime最小），
     * 并尝试自动打开该物品的背包界面进行操作。
     * 如果没有可用物品，则在游戏界面显示“背包已满，请先清理”的提示。
     */
    public static void checkItems() {
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
        } else client.inGameHud.setOverlayMessage(Text.of("背包已满，请先清理"),false);
    }
    public static void reSwitchItem() {
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