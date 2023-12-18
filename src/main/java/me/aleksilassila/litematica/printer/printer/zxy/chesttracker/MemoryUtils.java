package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import red.jackf.chesttracker.api.events.AfterPlayerDestroyBlock;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.chesttracker.provider.ProviderHandler;
import red.jackf.chesttracker.storage.Storage;

public class MemoryUtils {
    public static MemoryBank PRINTER_MEMORY = null;
    public void deleteCurrentStorage(){
        if(MemoryBank.INSTANCE !=null) Storage.delete(MemoryBank.INSTANCE.getId());
    }

    public static void setup() {
        AfterPlayerDestroyBlock.EVENT.register(cbs -> {
            // Called when a player breaks a block, to remove memories that would be contained there
            if (PRINTER_MEMORY != null
                    && PRINTER_MEMORY.getMetadata().getIntegritySettings().removeOnPlayerBlockBreak
            ) {
                var currentKey = ProviderHandler.getCurrentKey();
                if (currentKey != null) {
                    PRINTER_MEMORY.removeMemory(currentKey, cbs.pos());
//                    LOGGER.debug("Player Destroy Block: Removing {}@{}", cbs.pos().toShortString(), currentKey);
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?>) {
                ScreenEvents.remove(screen).register(screen1 -> {
                    SaveMemory.save((HandledScreen<?>) screen1);
                });
            }
        });
    }
}