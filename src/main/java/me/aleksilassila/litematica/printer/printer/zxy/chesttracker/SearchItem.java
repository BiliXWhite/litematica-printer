package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.client.api.events.SearchRequestPopulator;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;

public class SearchItem {
    static AtomicBoolean hasItem = new AtomicBoolean(false);

    public static boolean search(boolean isPrinterMemory) {
        MemoryBank memoryBank = isPrinterMemory ? PRINTER_MEMORY : MemoryBank.INSTANCE;
        if (memoryBank != null) {
            Map<Identifier, Map<BlockPos, Memory>> memories = memoryBank.getMemories();
            if (Statistics.currentMemoryKey != null) {
                //搜索当前选中的维度
                memoriesSearch(Statistics.currentMemoryKey,Statistics.itemStack,memoryBank);
                //搜索全部维度
                memories.keySet().forEach(key -> {
                    if (!hasItem.get() && !key.equals(Statistics.currentMemoryKey)) memoriesSearch(key,Statistics.itemStack,memoryBank);
                });
            }
            if(hasItem.get()) {
                if(isPrinterMemory) Statistics.closeScreen++;
                hasItem.set(false);
                return true;
            }
            hasItem.set(false);
        }
        return false;
    }

    public static void memoriesSearch(Identifier key, ItemStack itemStack,MemoryBank memoryBank) {
        if (key == null || itemStack == null) return;
        if (memoryBank != null && !MemoryBank.ENDER_CHEST_KEY.equals(key)) {
            SearchRequest searchRequest = new SearchRequest();
            SearchRequestPopulator.addItemStack(searchRequest, itemStack, SearchRequestPopulator.Context.FAVOURITE);
            memoryBank.getPositions(key, searchRequest).
                    forEach(v -> {
                        if (v.item() != null && InventoryUtils.areStacksEqual(itemStack, v.item()) && !hasItem.get()) {
                            OpenInventoryPacket.sendOpenInventory(v.pos(), RegistryKey.of(RegistryKeys.WORLD, key));
                            hasItem.set(true);
                        }
                    });
        }
    }
}
