package me.aleksilassila.litematica.printer.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MessageUtils {

    public static final Minecraft client = Minecraft.getInstance();

    public static void setOverlayMessage(Component message, boolean bl) {
        client.gui.setOverlayMessage(message, bl);
    }

    public static void addMessage(Component message) {
        client.gui.getChat().addMessage(message);
    }

    public static void setOverlayMessage(Component message) {
        client.gui.setOverlayMessage(message, false);
    }

    // 扩展方法，普通字符串形式, 但并不建议使用, 因为没有做I18n
    public static void setOverlayMessage(String message) {
        setOverlayMessage(StringUtils.literal(message));
    }

    // 扩展方法，普通字符串形式, 但并不建议使用, 因为没有做I18n
    public static void addMessage(String message) {
        addMessage(StringUtils.literal(message));
    }
}
