package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.Messager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import net.minecraft.client.MinecraftClient;

//#if MC > 12001
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

        return false;
    }
}
