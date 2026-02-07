package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;

//#if MC >= 12001 
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import fi.dy.masa.malilib.util.GuiUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import net.minecraft.resources.Identifier;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
//#elseif MC < 12001
//$$ import net.minecraft.network.chat.Component;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//#endif


//监听按键
public class HotkeysCallback {
    private static final Minecraft client = Minecraft.getInstance();

    public static boolean onKeyAction(KeyAction action, IKeybind key) {
        if (client.player == null || client.level == null) {
            return false;
        }
        if (key == Configs.Hotkeys.OPEN_SCREEN.getKeybind()) {
            client.setScreen(new ConfigUi());
            return true;
        }

        if (key == Configs.Hotkeys.SYNC_INVENTORY.getKeybind()) {
            ZxyUtils.startOrOffSyncInventory();
            return true;
        }

        if (key == Configs.Hotkeys.PRINTER_INVENTORY.getKeybind()) {
            ZxyUtils.startAddPrinterInventory();
            return true;
        }

        if (key == Configs.Hotkeys.REMOVE_PRINT_INVENTORY.getKeybind()) {
            //#if MC >= 12001 
            MemoryUtils.deletePrinterMemory();
            //#elseif MC < 12001
            //$$ MemoryDatabase database = MemoryDatabase.getCurrent();
            //$$ if (database != null) {
            //$$ for (ResourceLocation dimension : database.getDimensions()) {
            //$$     database.clearDimension(dimension);
            //$$     }
            //$$ }
            //$$ client.gui.setOverlayMessage(Component.nullToEmpty("打印机库存已清空"), false);
            //#endif
            return true;
        }

        //#if MC >= 12001 
        if (GuiUtils.getCurrentScreen() instanceof AbstractContainerScreen<?> && !(GuiUtils.getCurrentScreen() instanceof CreativeModeInventoryScreen)) {
            if (key == Configs.Hotkeys.LAST.getKeybind()) {
                SearchItem.page = --SearchItem.page <= -1 ? SearchItem.maxPage - 1 : SearchItem.page;
                SearchItem.openInventory(SearchItem.page);
            } else if (key == Configs.Hotkeys.NEXT.getKeybind()) {
                SearchItem.page = ++SearchItem.page >= SearchItem.maxPage ? 0 : SearchItem.page;
                SearchItem.openInventory(SearchItem.page);
            } else if (key == Configs.Hotkeys.DELETE.getKeybind()) {
                MemoryBankImpl memoryBank = MemoryBankAccessImpl.INSTANCE.getLoadedInternal().orElse(null);
                if (memoryBank != null && OpenInventoryPacket.key != null && client.player != null) {
                    memoryBank.removeMemory(OpenInventoryPacket.key.identifier(), OpenInventoryPacket.pos);
                    OpenInventoryPacket.key = null;
                    client.player.closeContainer();
                }
            }
        }
        //#endif

        return false;
    }
}
