package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.client.api.events.SearchRequestPopulator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;
import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.currentMemoryKey;

public class SearchItem {
    static AtomicBoolean hasItem = new AtomicBoolean(false);
    static NbtCompound nbt = new NbtCompound();

    public static boolean search(boolean isPrinterMemory) {
        MemoryBank memoryBank = isPrinterMemory ? PRINTER_MEMORY : MemoryBank.INSTANCE;
        if (memoryBank != null) {
            Map<Identifier, Map<BlockPos, Memory>> memories = memoryBank.getMemories();
            if (currentMemoryKey != null) {
                //搜索当前选中的维度
                memoriesSearch(currentMemoryKey, MemoryUtils.itemStack, memoryBank);
                //搜索全部维度
                memories.keySet().forEach(key -> {
                    if (!hasItem.get() && !key.equals(currentMemoryKey))
                        memoriesSearch(key, MemoryUtils.itemStack, memoryBank);
                });
            }
            if (hasItem.get()) {
                hasItem.set(false);
                return true;
            }
            hasItem.set(false);
        }
        return false;
    }

    public static void memoriesSearch(Identifier key, ItemStack itemStack, MemoryBank memoryBank) {
        if (key == null || itemStack == null) return;
        if (memoryBank != null && !MemoryBank.ENDER_CHEST_KEY.equals(key)) {
            SearchRequest searchRequest = new SearchRequest();
            SearchRequestPopulator.addItemStack(searchRequest, itemStack, SearchRequestPopulator.Context.FAVOURITE);
            memoryBank.getPositions(key, searchRequest).
                    forEach(v -> {
                        if (v.item() != null &&
                                !hasItem.get()) {
                            OpenInventoryPacket.sendOpenInventory(v.pos(), RegistryKey.of(RegistryKeys.WORLD, key));
                            hasItem.set(true);
                        }
                    });
        }
    }

    public static boolean areStacksEquivalent(@NotNull ItemStack stack1, @NotNull ItemStack memoryStack) {
        /*if (!Registries.ITEM.getId(stack1.getItem()).toString().contains("shulker_box")) {
            return  Registries.ITEM.getId(memoryStack.getItem()).toString().contains("shulker_box") &&
                    InventoryUtils.getStoredItems(memoryStack).stream().anyMatch(mStack ->
                            stack1.getName().getString().equals(mStack.getName().getString()) && InventoryUtils.areStacksEqual(stack1, mStack));
        } else */

        if (Registries.ITEM.getId(stack1.getItem()).toString().contains("shulker_box") && Registries.ITEM.getId(memoryStack.getItem()).toString().contains("shulker_box")) {
            return (InventoryUtils.getStoredItems(stack1).isEmpty() && InventoryUtils.getStoredItems(memoryStack).isEmpty() && stack1.getName().getString().equals(memoryStack.getName().getString())) ||
                    (!InventoryUtils.getStoredItems(stack1).isEmpty() &&
                            !InventoryUtils.getStoredItems(memoryStack).isEmpty() &&
                            stack1.getName().getString().equals(memoryStack.getName().getString()) &&
                            compArray(InventoryUtils.getStoredItems(stack1, -1), InventoryUtils.getStoredItems(memoryStack, -1)));
        }else if(Registries.ITEM.getId(memoryStack.getItem()).toString().contains("shulker_box")){
            return true;
        }
        return stack1.getName().getString().equals(memoryStack.getName().getString());
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