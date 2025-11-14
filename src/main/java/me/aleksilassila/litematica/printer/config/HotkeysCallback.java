package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.MinecraftClient;


import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;

//#if MC >= 12001
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
//#else
//$$ import net.minecraft.text.Text;
//$$ import net.minecraft.util.Identifier;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//#endif

import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.startAddPrinterInventory;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.startOrOffSyncInventory;

import static me.aleksilassila.litematica.printer.config.Configs.OPEN_SCREEN;

//监听按键
public class HotkeysCallback implements IHotkeyCallback {
    MinecraftClient client = MinecraftClient.getInstance();

    //激活的热键会被key记录
    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        if (this.client.player == null || this.client.world == null) return false;
        if(key == OPEN_SCREEN.getKeybind()){
            client.setScreen(new ConfigUi());
            return true;
        }else if(key == SYNC_INVENTORY.getKeybind()){
            startOrOffSyncInventory();
            return true;
        }else if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE) && key == SWITCH_PRINTER_MODE.getKeybind()){
            IConfigOptionListEntry cycle = PRINTER_MODE.getOptionListValue().cycle(true);
            PRINTER_MODE.setOptionListValue(cycle);
            ZxyUtils.actionBar(PRINTER_MODE.getOptionListValue().getDisplayName());
        }else if(key == PRINTER_INVENTORY.getKeybind()){
            startAddPrinterInventory();
            return true;
        }else if(key == REMOVE_PRINT_INVENTORY.getKeybind()){
            //#if MC >= 12001
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
        //#if MC >= 12001
        else if(GuiUtils.getCurrentScreen() instanceof HandledScreen<?> &&
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
                MemoryBankImpl memoryBank = MemoryBankAccessImpl.INSTANCE.getLoadedInternal().orElse(null);
                if (memoryBank!= null && OpenInventoryPacket.key != null && client.player != null) {
                    memoryBank.removeMemory(OpenInventoryPacket.key.getValue(),OpenInventoryPacket.pos);
                    OpenInventoryPacket.key = null;
                    client.player.closeHandledScreen();
                }
            }
        }
        //#endif
        else if (key == TOGGLE_PRINTING_MODE.getKeybind()) {
            StringUtils.printChatMessage("开关打印机时调用");
            BreakManager.instance().clear();
        }
        return false;
    }

    //设置反馈到onKeyAction()方法的快捷键
    public static void init(){
        HotkeysCallback hotkeysCallback = new HotkeysCallback();

        for (ConfigHotkey configHotkey : Configs.addKeyList()) {
            configHotkey.getKeybind().setCallback(hotkeysCallback);
        }
    }
}
