package me.aleksilassila.litematica.printer.bilixwhite.utils;

import fi.dy.masa.litematica.Litematica;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
//#if MC <= 11802
//$$ import net.minecraft.text.TranslatableText;
//#endif
public class StringUtils {


    public static final MinecraftClient client = MinecraftClient.getInstance();
    private static MatrixStack matrices;
    private static DrawContext drawContext;

    public static void printChatMessage(String message) {
        client.inGameHud.getChatHud().addMessage(Text.of(message));
    }

    public static void info(String message) {
        Litematica.
        //#if MC < 12104 && MC != 12101
        //$$ logger
        //#else
        LOGGER
        //#endif
        .info("[Printer] {}", message);
    }


    public static void initMatrix(MatrixStack matrix) {
        matrices = matrix;
    }

    public static void initDrawContext(DrawContext context) {
        drawContext = context;
    }

    public static void drawText(String text, int x, int y, int color) {
        drawText(text, x, y, color, false);
    }


    public static void drawText(String text, int x, int y, int color, boolean withShadow) {
        drawText(text, x, y, color, withShadow, false);
    }

    public static void drawText(String text, int x, int y, int color, boolean withShadow, boolean centered) {
        if (centered) x -= client.textRenderer.getWidth(text) / 2;
        //#if MC > 11904
        drawContext.drawText(client.textRenderer, text, x, y, color, withShadow);
        //#else
        //$$ if (matrices == null)
        //$$     throw new NullPointerException("MatrixStack is null");
        //$$ if (withShadow)
        //$$     client.textRenderer.drawWithShadow(matrices, text, x, y, color);
        //$$ else client.textRenderer.draw(matrices, text, x, y, color);
        //#endif
    }

    public static String getName(String key) {
        //#if MC > 11802
        return Text.translatable(LitematicaMixinMod.MOD_ID + ".config.name." + key).getString();
        //#else
        //$$ return new TranslatableText(LitematicaMixinMod.MOD_ID + ".config.name." + key).getString();
        //#endif
    }

    public static String getComment(String key) {
        //#if MC > 11802
        return Text.translatable(LitematicaMixinMod.MOD_ID + ".config.comment." + key).getString();
        //#else
        //$$ return new TranslatableText(LitematicaMixinMod.MOD_ID + ".config.comment." + key).getString();
        //#endif
    }

    public static String get(String key) {
        //#if MC > 11802
        return Text.translatable(LitematicaMixinMod.MOD_ID + "." + key).getString();
        //#else
        //$$ return new TranslatableText(LitematicaMixinMod.MOD_ID + "." + key).getString();
        //#endif
    }
}
