package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import net.minecraft.client.MinecraftClient;

//#if MC > 12001
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import red.jackf.chesttracker.memory.MemoryBank;
//#else
//$$ import net.minecraft.text.Text;
//$$ import net.minecraft.util.Identifier;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//#endif

import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.startAddPrinterInventory;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.startOrOffSyncInventory;

public class KeyCallbackHotkeys implements IHotkeyCallback {
    private final MinecraftClient client;

    public KeyCallbackHotkeys(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        if (this.client.player == null || this.client.world == null) return false;

        if(key == SYNC_INVENTORY.getKeybind()){
            startOrOffSyncInventory();
            return true;
        }else if(key == PRINTER_INVENTORY.getKeybind()){
            startAddPrinterInventory();
            return true;
        }else if(key == REMOVE_PRINT_INVENTORY.getKeybind()){
            //#if MC > 12001
            MemoryUtils.deletePrinterMemory();
            //#else
            //$$ MemoryDatabase database = MemoryDatabase.getCurrent();
            //$$ if (database != null) {
            //$$     for (Identifier dimension : database.getDimensions()) {
            //$$         database.clearDimension(dimension);
            //$$     }
            //$$ }
            //$$ client.inGameHud.setOverlayMessage(Text.of("打印机库存已清空"), false);
            //#endif
            return true;
        }
        //#if MC > 12001
        else if(GuiUtils.getCurrentScreen() instanceof HandledScreen<?> gui &&
                !(GuiUtils.getCurrentScreen() instanceof CreativeInventoryScreen))
        {
            if(key == LAST.getKeybind()){
                SearchItem.page = --SearchItem.page <= -1 ? SearchItem.maxPage-1 : SearchItem.page;
                SearchItem.openInventory(SearchItem.page);
            }
            else if(key == NEXT.getKeybind()){
                SearchItem.page = ++SearchItem.page >= SearchItem.maxPage ? 0 : SearchItem.page;
                SearchItem.openInventory(SearchItem.page);
            }
            else if(key == DELETE.getKeybind()){
                if (MemoryBank.INSTANCE != null && OpenInventoryPacket.key != null && client.player != null) {
                    MemoryBank.INSTANCE.removeMemory(OpenInventoryPacket.key.getValue(),OpenInventoryPacket.pos);
                    OpenInventoryPacket.key = null;
                    client.player.closeHandledScreen();
                }
            }
        }
        //#endif
        return false;
    }
}
