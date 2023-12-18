package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Statistics;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import red.jackf.chesttracker.api.provider.MemoryBuilder;
import red.jackf.chesttracker.api.provider.ProviderUtils;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.jackfredlib.api.base.ResultHolder;

import java.util.List;

public class SaveMemory {
    public static void save(HandledScreen<?> screen){
        List<ItemStack> items = ProviderUtils.getNonPlayerStacksAsList(screen);
        ResultHolder<MemoryBuilder.Entry> value = ResultHolder.value(MemoryBuilder.create(items)
                .inContainer(Statistics.blockState.getBlock())
                .toEntry(OpenInventoryPacket.key.getValue(), OpenInventoryPacket.pos)
        );
        if (MemoryBank.INSTANCE != null) {
            MemoryBank.INSTANCE.addMemory(value.get());
        }
        OpenInventoryPacket.key = null;
        OpenInventoryPacket.pos = null;
    }
}