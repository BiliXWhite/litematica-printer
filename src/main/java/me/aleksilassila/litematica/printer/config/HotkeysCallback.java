package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import net.minecraft.client.Minecraft;

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

        return false;
    }
}