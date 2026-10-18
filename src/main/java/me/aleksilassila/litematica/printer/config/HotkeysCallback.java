package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import me.aleksilassila.litematica.printer.utils.mods.ChestTrackerBridge;
import net.minecraft.client.Minecraft;


//监听按键
public class HotkeysCallback {
    private static final Minecraft client = Minecraft.getInstance();

    public static boolean onKeyAction(KeyAction action, IKeybind key) {
        if (client.player == null || client.level == null) {
            return false;
        }
        if (key == Configs.Hotkeys.OPEN_SCREEN.getKeybind()) {
            //#if MC > 260100
            //$$ client.gui.setScreen(new ConfigUi());
            //#else
            client.setScreen(new ConfigUi());
            //#endif
            return true;
        }

        if (key == Configs.Hotkeys.SYNC_INVENTORY.getKeybind()) {
            me.aleksilassila.litematica.printer.runtime.RuntimeAccess.get().containerSync().toggle();
            return true;
        }
        if (key == Configs.Hotkeys.CACHE_SELECTION_CONTAINERS.getKeybind()) {
            int added = ChestTrackerBridge.addSelectionToCache();
            me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils.setOverlayMessage(
                    "Chest Tracker: 已加入 " + added + " 个选区容器");
            return true;
        }
        if (key == Configs.Hotkeys.CLEAR_CONTAINER_CACHE.getKeybind()) {
            int removed = ChestTrackerBridge.clearSelectionCache();
            me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils.setOverlayMessage(
                    "Chest Tracker: 已清除 " + removed + " 个容器缓存");
            return true;
        }

        return false;
    }
}
