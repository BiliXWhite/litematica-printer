package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
            //$$ MemoryUtils.deletePrinterMemory();
            //#else
            MemoryDatabase database = MemoryDatabase.getCurrent();
            if (database != null) {
                for (Identifier dimension : database.getDimensions()) {
                    database.clearDimension(dimension);
                }
            }
            client.inGameHud.setOverlayMessage(Text.of("打印机库存已清空"), false);
            //#endif
            return true;
        }
        return false;
    }
}
