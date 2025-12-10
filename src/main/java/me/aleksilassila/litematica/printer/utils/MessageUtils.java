package me.aleksilassila.litematica.printer.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MessageUtils {

    public static final Minecraft client = Minecraft.getInstance();

    public static void setOverlayMessage(Component message) {
        client.gui.setOverlayMessage(message, false);
    }

    public static void addMessage(Component message) {
        client.gui.getChat().addMessage(message);
    }
}
