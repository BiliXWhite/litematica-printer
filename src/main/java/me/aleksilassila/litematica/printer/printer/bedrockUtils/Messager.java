package me.aleksilassila.litematica.printer.printer.bedrockUtils;


import net.minecraft.client.MinecraftClient;

import net.minecraft.text.Text;
//#if MC > 11802
//$$ import net.minecraft.text.MutableText;
//#else
import net.minecraft.text.TranslatableText;
//#endif
public class Messager {
    public static void actionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //#if MC > 11802
        //$$ MutableText translatable = Text.translatable(message);
        //#else
        TranslatableText translatable = new TranslatableText(message);
        //#endif
        minecraftClient.inGameHud.setOverlayMessage(translatable,false);
    }
    public static void rawactionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //#if MC > 11802
        //$$ MutableText translatable = Text.translatable(message);
        //#else
        TranslatableText translatable = new TranslatableText(message);
        //#endif
        minecraftClient.inGameHud.setOverlayMessage(translatable,false);
    }

    public static void chat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //#if MC > 11802
        //$$ MutableText translatable = Text.translatable(message);
        //#else
        TranslatableText translatable = new TranslatableText(message);
        //#endif
        minecraftClient.inGameHud.getChatHud().addMessage(translatable);
    }

    public static void rawchat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
//        Text text = new ofText(message);
//        minecraftClient.inGameHud.addChatMessage(MessageType.SYSTEM,text, UUID.randomUUID());
    }
}

