package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.client.api.events.SearchRequestPopulator;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.*;

public class SearchItem {
    static AtomicBoolean hasItem = new AtomicBoolean(false);
    static NbtCompound nbt = new NbtCompound();

    public static boolean search(boolean isPrinterMemory) {
        MemoryBank memoryBank = isPrinterMemory ? PRINTER_MEMORY : MemoryBank.INSTANCE;
        if (memoryBank != null) {
            Map<Identifier, Map<BlockPos, Memory>> memories = memoryBank.getMemories();
            if (currentMemoryKey != null) {
                //搜索当前选中的维度
                memoriesSearch(currentMemoryKey,MemoryUtils.itemStack,memoryBank);
                //搜索全部维度
                memories.keySet().forEach(key -> {
                    if (!hasItem.get() && !key.equals(currentMemoryKey)) memoriesSearch(key,MemoryUtils.itemStack,memoryBank);
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
                        if (v.item() != null &&
                                areStacksEquivalent(itemStack, v.item()) &&
                                SearchRequest.check(v.item(),request) &&
                                !hasItem.get()) {
                            OpenInventoryPacket.sendOpenInventory(v.pos(), RegistryKey.of(RegistryKeys.WORLD, key));
                            hasItem.set(true);
                        }
                    });
        }
    }
    public static boolean areStacksEquivalent(@NotNull ItemStack stack1, @NotNull ItemStack stack2) {
        return
//                stack1.getItem() == stack2.getItem() &&
                stack1.getName().getString().equals(stack2.getName().getString());
//                && (ignoreNbt || !stack1.hasNbt() && !stack2.hasNbt() || Objects.equals(stack1.getNbt(), stack2.getNbt()));
    }
}