package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

//#if MC >= 12001
import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.NotNull;
import red.jackf.chesttracker.api.memory.CommonKeys;
import red.jackf.chesttracker.api.memory.Memory;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
import red.jackf.chesttracker.impl.memory.MemoryKeyImpl;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.client.api.events.SearchRequestPopulator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SearchItem {
    @NotNull
    static Minecraft client = Minecraft.getInstance();
    static AtomicBoolean hasItem = new AtomicBoolean(false);
    static boolean isPrinterMemory = false;
    static Map<ResourceLocation,Map<BlockPos, Memory>> currItems = new LinkedHashMap<>();
    public static int page = 0;
    public static int maxPage = 0;
    public static void initPage(){
        if (currItems.isEmpty()) return;
        maxPage = 0;
        page = 0;
        currItems.forEach((k,v) -> {
            if(v == null)return;
            maxPage+=v.size();
        });
    }
    public static void openInventory(int p){
        if(currItems.isEmpty() || client.player == null)return;
        client.player.closeContainer();
        final int[] pageFix = {p};
        currItems.forEach((k,v) -> {
            if(OpenInventoryPacket.key!=null || v == null || k == null) return;

            if(pageFix[0] > v.size() - 1){
                pageFix[0] -= v.size();
            }else {
                v.entrySet().stream().skip(pageFix[0]).findFirst().ifPresent((value) ->
                        OpenInventoryPacket.sendOpenInventory(value.getKey(),ResourceKey.create(Registries.DIMENSION, k)));
            }
//            v.forEach((k1,v1) -> {
//                if(OpenInventoryPacket.key!=null) return;
//                if(pageFix[0] != 0){
//                    pageFix[0]--;
//                }else if(k!=null && k1 != null){
//                    OpenInventoryPacket.sendOpenInventory(k1,RegistryKey.of(RegistryKeys.WORLD, k));
//                }
//            });
        });
    }

    public static boolean search(boolean isPrinterMemory) {
        SearchItem.isPrinterMemory = isPrinterMemory;
        MemoryBankImpl memoryBank = isPrinterMemory ? MemoryUtils.PRINTER_MEMORY : MemoryBankAccessImpl.INSTANCE.getLoadedInternal().orElse(null);
        if (memoryBank != null) {
            Map<ResourceLocation, MemoryKeyImpl> memories = memoryBank.getMemories();
            if (MemoryUtils.currentMemoryKey != null) {
                Map<ResourceLocation,Map<BlockPos,Memory>> itemMemoryMap = new LinkedHashMap<>();
                //搜索当前选中的维度
                Map<BlockPos, Memory> blockPosMemoryMap = memoriesSearch(MemoryUtils.currentMemoryKey, MemoryUtils.itemStack, memoryBank);
                itemMemoryMap.put(MemoryUtils.currentMemoryKey,blockPosMemoryMap);

                //搜索全部维度
                memories.keySet().forEach(key -> {
                    if (!hasItem.get() && !key.equals(MemoryUtils.currentMemoryKey)) {
                        Map<BlockPos, Memory> blockPosMemoryMap1 = memoriesSearch(key, MemoryUtils.itemStack, memoryBank);
                        itemMemoryMap.put(key,blockPosMemoryMap1);
                    }
                });
                currItems = itemMemoryMap;
                initPage();
            }
            if (hasItem.get()) {
                hasItem.set(false);
                return true;
            }
            hasItem.set(false);
        }
        return false;
    }

    public static Map<BlockPos,Memory> memoriesSearch(ResourceLocation key, ItemStack itemStack, MemoryBankImpl memoryBank) {
        if (key == null || itemStack == null) return null;
        LocalPlayer player = client.player;
        if (player == null) return null;
        if (memoryBank != null && memoryBank.getMemories() != null &&
                memoryBank.getMemories().get(key) != null &&
                !CommonKeys.ENDER_CHEST_KEY.equals(key)) {
            SearchRequest searchRequest = new SearchRequest();
            SearchRequestPopulator.addItemStack(searchRequest, itemStack, SearchRequestPopulator.Context.FAVOURITE);
            int range = memoryBank.getMetadata().getSearchSettings().searchRange;
            double rangeSquared = range == Integer.MAX_VALUE ? Integer.MAX_VALUE : range * range;

            Map<BlockPos,Memory> itemsMap = new LinkedHashMap<>();
            for (Map.Entry<BlockPos, Memory> entry : memoryBank.getMemories().get(key).getMemories().entrySet()) {
                if (entry.getKey().distToCenterSqr(player.trackingPosition()) > rangeSquared && range != Integer.MAX_VALUE) continue;
                if (entry.getValue().items().stream()
                        .filter(item -> SearchRequest.check(item, searchRequest))
                        .anyMatch(item -> !isPrinterMemory || !((Block.byItem(item.getItem())) instanceof ShulkerBoxBlock))) {
                    if(isPrinterMemory){
                        OpenInventoryPacket.sendOpenInventory(entry.getKey(), ResourceKey.create(Registries.DIMENSION, key));
                        hasItem.set(true);
                        return null;
                    }
                    itemsMap.put(entry.getKey(),entry.getValue());
                }
            }
            return itemsMap;
        }
        return null;
    }

    public static boolean areStacksEquivalent(@NotNull ItemStack stack1, @NotNull ItemStack memoryStack) {
        /*if (!Registries.ITEM.getId(stack1.getItem()).toString().contains("shulker_box")) {
            return  Registries.ITEM.getId(memoryStack.getItem()).toString().contains("shulker_box") &&
                    InventoryUtils.getStoredItems(memoryStack).stream().anyMatch(mStack ->
                            stack1.getName().getString().equals(mStack.getName().getString()) && InventoryUtils.areStacksEqual(stack1, mStack));
        } else */

        if (BuiltInRegistries.ITEM.getKey(stack1.getItem()).toString().contains("shulker_box") && BuiltInRegistries.ITEM.getKey(memoryStack.getItem()).toString().contains("shulker_box")) {
            return (InventoryUtils.getStoredItems(stack1).isEmpty() && InventoryUtils.getStoredItems(memoryStack).isEmpty() && stack1.getHoverName().getString().equals(memoryStack.getHoverName().getString())) ||
                    (!InventoryUtils.getStoredItems(stack1).isEmpty() &&
                            !InventoryUtils.getStoredItems(memoryStack).isEmpty() &&
                            stack1.getHoverName().getString().equals(memoryStack.getHoverName().getString()) &&
                            compArray(InventoryUtils.getStoredItems(stack1, -1), InventoryUtils.getStoredItems(memoryStack, -1)));
        } else if (BuiltInRegistries.ITEM.getKey(memoryStack.getItem()).toString().contains("shulker_box")) {
            return true;
        }
        return stack1.getHoverName().getString().equals(memoryStack.getHoverName().getString());
//                && (ignoreNbt || !stack1.hasNbt() && !stack2.hasNbt() || Objects.equals(stack1.getNbt(), stack2.getNbt()));
    }

    private static boolean compArray(List<ItemStack> list1, List<ItemStack> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!InventoryUtils.areStacksEqual(list1.get(i), list2.get(i))) return false;
        }
        return true;
    }
}
//#endif