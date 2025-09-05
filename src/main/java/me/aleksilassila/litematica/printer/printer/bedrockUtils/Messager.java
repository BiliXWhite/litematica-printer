package me.aleksilassila.litematica.printer.printer.bedrockUtils;


import net.minecraft.client.MinecraftClient;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import net.minecraft.text.MutableText;
//#if MC >= 12105
import java.net.URI;
//#endif

//#if MC > 11802
//#else
//$$ import net.minecraft.text.TranslatableText;
//#endif
public class Messager {
    public static void actionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //#if MC > 11802
        MutableText translatable = Text.translatable(message);
        //#else
        //$$ TranslatableText translatable = new TranslatableText(message);
        //#endif
        minecraftClient.inGameHud.setOverlayMessage(translatable,false);
    }
    public static void rawactionBar(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //#if MC > 11802
        MutableText translatable = Text.translatable(message);
        //#else
        //$$ TranslatableText translatable = new TranslatableText(message);
        //#endif
        minecraftClient.inGameHud.setOverlayMessage(translatable,false);
    }

    public static void chat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //#if MC > 11802
        MutableText translatable = Text.translatable(message);
        //#else
        //$$ TranslatableText translatable = new TranslatableText(message);
        //#endif
        minecraftClient.inGameHud.getChatHud().addMessage(translatable);
    }

    public static void rawchat(String message){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
//        Text text = new ofText(message);
//        minecraftClient.inGameHud.addChatMessage(MessageType.SYSTEM,text, UUID.randomUUID());
    }

    public static @NotNull MutableText createOpenUrlText(String text, String url) {
        MutableText bv = Text.of(text).copy();
        bv.styled(style -> style.withColor(Formatting.GOLD));
        bv.styled(style -> style.withUnderline(true));
        //#if MC >= 12105
        bv.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.of("点击打开："+url))));
        bv.styled(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url))));
        //#else
        //$$ bv.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("点击打开："+url))));
        //$$ bv.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        //#endif
        return bv;
    }
}

