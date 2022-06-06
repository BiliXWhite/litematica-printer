package me.aleksilassila.litematica.printer.printer.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.UUID;

public class Messager {
    public static void actionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.inGameHud.setOverlayMessage(new TranslatableText(message),false);
    }
    public static void rawactionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Text text = new LiteralText(message);
        minecraftClient.inGameHud.setOverlayMessage(text,false);
    }

    public static void chat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.inGameHud.getChatHud().addMessage(new TranslatableText(message));
    }

    public static void rawchat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Text text = new LiteralText(message);
        minecraftClient.inGameHud.addChatMessage(MessageType.SYSTEM,text, UUID.randomUUID());
    }
}

