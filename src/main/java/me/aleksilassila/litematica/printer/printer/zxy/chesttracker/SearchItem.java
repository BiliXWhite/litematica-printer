package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Statistics;
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

public class SearchItem {
    static AtomicBoolean hasItem = new AtomicBoolean(false);

    public static void search() {
        MemoryBank memoryBank = MemoryBank.INSTANCE;
        if (memoryBank != null) {
            Map<Identifier, Map<BlockPos, Memory>> memories = memoryBank.getMemories();
            if (MemoryBank.INSTANCE != null && Statistics.currentMemoryKey != null) {
                //搜索当前选中的维度
                memoriesSearch(Statistics.currentMemoryKey);
                //搜索全部维度
                memories.keySet().forEach(key -> {
                    if (!hasItem.get() && !key.equals(Statistics.currentMemoryKey)) memoriesSearch(key);
                });
            }
            hasItem.set(false);
        }
    }

    private static void memoriesSearch(Identifier key) {
        if (MemoryBank.INSTANCE != null && !key.equals(MemoryBank.ENDER_CHEST_KEY)) {
            SearchRequest searchRequest = new SearchRequest();
            SearchRequestPopulator.addItemStack(searchRequest, Statistics.itemStack, SearchRequestPopulator.Context.FAVOURITE);
            MemoryBank.INSTANCE.getPositions(key, searchRequest).
                    forEach(v -> {
                        if (v.item() != null && InventoryUtils.areStacksEqual(Statistics.itemStack, v.item()) && !hasItem.get()) {
//                    if (MinecraftClient.getInstance().world != null) {
//                        InteractionTrackerImpl.INSTANCE.setLastBlockSource(new CachedClientBlockSource(MinecraftClient.getInstance().world, v.pos()));
//                    }
                            OpenInventoryPacket.sendOpenInventory(v.pos(), RegistryKey.of(RegistryKeys.WORLD, key));
                            hasItem.set(true);
                        }
                    });
        }
    }
}
