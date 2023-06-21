package me.aleksilassila.litematica.printer.printer.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.UUID;

public class Messager {
    public static void actionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.inGameHud.setOverlayMessage(Text.of(message),false);
    }
    public static void rawactionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.inGameHud.setOverlayMessage(Text.of(message),false);
    }

    public static void chat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.inGameHud.getChatHud().addMessage(Text.of(message));
    }

    public static void rawchat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
//        Text text = new LiteralText(message);
//        minecraftClient.inGameHud.addChatMessage(MessageType.SYSTEM,text, UUID.randomUUID());
    }
}

