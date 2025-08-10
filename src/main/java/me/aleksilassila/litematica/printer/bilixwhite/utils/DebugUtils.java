package me.aleksilassila.litematica.printer.bilixwhite.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class DebugUtils {
    public static void print(Object obj) {
        System.out.println(obj);
    }

    public static void printChatMessage(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(message));
    }
}
