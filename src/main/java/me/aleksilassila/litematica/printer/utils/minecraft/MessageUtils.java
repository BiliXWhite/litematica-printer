package me.aleksilassila.litematica.printer.utils.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MessageUtils {
    public static void setOverlayMessage(Component message, boolean bl) {
        Minecraft.getInstance().gui.setOverlayMessage(message, bl);
    }

    public static void setOverlayMessage(Component message) {
        setOverlayMessage(message, false);
    }

    public static void setOverlayMessage(String message) {
        setOverlayMessage(StringUtils.literal(message));
    }

    public static void addMessage(Component message) {
        //#if MC>=260000
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(message);
        //#else
        //$$ Minecraft.getInstance().gui.getChat().addMessage(message);
        //#endif
    }

    public static void addMessage(String message) {
        addMessage(StringUtils.literal(message));
    }
}
