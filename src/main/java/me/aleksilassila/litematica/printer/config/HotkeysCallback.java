package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.config.enums.ModeType;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;

//#if MC >= 12001 && MC <= 12104
//$$ import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//$$ import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
//$$ import fi.dy.masa.malilib.util.GuiUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
//$$ import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
//#elseif MC < 12001
//$$ import net.minecraft.network.chat.Component;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//#endif

import static me.aleksilassila.litematica.printer.InitHandler.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.startAddPrinterInventory;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.startOrOffSyncInventory;

//监听按键
public class HotkeysCallback implements IHotkeyCallback {
    Minecraft client = Minecraft.getInstance();

    //激活的热键会被key记录
    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        if (this.client.player == null || this.client.level == null) {
            return false;
        }
        if (key == OPEN_SCREEN.getKeybind()) {
            client.setScreen(new ConfigUI());
            return true;
        } else if (key == SYNC_INVENTORY.getKeybind()) {
            startOrOffSyncInventory();
            return true;
        } else if (MODE_SWITCH.getOptionListValue().equals(ModeType.SINGLE) && key == SWITCH_PRINTER_MODE.getKeybind()) {
            IConfigOptionListEntry cycle = PRINTER_MODE.getOptionListValue().cycle(true);
            PRINTER_MODE.setOptionListValue(cycle);
            ZxyUtils.actionBar(PRINTER_MODE.getOptionListValue().getDisplayName());
        } else if (key == PRINTER_INVENTORY.getKeybind()) {
            startAddPrinterInventory();
            return true;
        } else if (key == REMOVE_PRINT_INVENTORY.getKeybind()) {
            //#if MC >= 12001 && MC <= 12104
            //$$ MemoryUtils.deletePrinterMemory();
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
        //#if MC >= 12001 && MC <= 12104
        //$$     else if (GuiUtils.getCurrentScreen() instanceof AbstractContainerScreen<?> &&
        //$$            !(GuiUtils.getCurrentScreen() instanceof CreativeModeInventoryScreen)) {
        //$$     if(key == LAST.getKeybind()){
        //$$         SearchItem.page = --SearchItem.page <= -1 ? SearchItem.maxPage-1 : SearchItem.page;
        //$$         SearchItem.openInventory(SearchItem.page);
        //$$     }
        //$$     else if(key == NEXT.getKeybind()){
        //$$         SearchItem.page = ++SearchItem.page >= SearchItem.maxPage ? 0 : SearchItem.page;
        //$$         SearchItem.openInventory(SearchItem.page);
        //$$     }
        //$$     else if(key == DELETE.getKeybind()){
        //$$         MemoryBankImpl memoryBank = MemoryBankAccessImpl.INSTANCE.getLoadedInternal().orElse(null);
        //$$         if (memoryBank!= null && OpenInventoryPacket.key != null && client.player != null) {
        //$$             memoryBank.removeMemory(OpenInventoryPacket.key.location(),OpenInventoryPacket.pos);
        //$$             OpenInventoryPacket.key = null;
        //$$             client.player.closeContainer();
        //$$         }
        //$$     }
        //$$ }
        //#endif
        return false;
    }

    //设置反馈到onKeyAction()方法的快捷键
    public static void init() {
        HotkeysCallback hotkeysCallback = new HotkeysCallback();
        for (ConfigHotkey configHotkey : Configs.getKeyList()) {
            configHotkey.getKeybind().setCallback(hotkeysCallback);
        }
    }
}
