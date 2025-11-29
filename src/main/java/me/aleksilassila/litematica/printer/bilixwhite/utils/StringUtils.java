package me.aleksilassila.litematica.printer.bilixwhite.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.litematica.Litematica;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

//#if MC <= 11802
//$$ import net.minecraft.network.chat.TranslatableComponent;
//#endif
public class StringUtils {


    public static final Minecraft client = Minecraft.getInstance();
    private static PoseStack poseStack;
    private static GuiGraphics guiGraphics;

    public static void printChatMessage(String message) {
        client.gui.getChat().addMessage(Component.nullToEmpty(message));
    }

    public static void info(String message) {
        Litematica.
        //#if MC < 12103 && MC != 12101
        //$$ logger
        //#else
        LOGGER
        //#endif
        .info("[Printer] {}", message);
    }


    public static void initMatrix(PoseStack poseStack) {
        StringUtils.poseStack = poseStack;
    }

    public static void initGuiGraphics(GuiGraphics guiGraphics) {
        StringUtils.guiGraphics = guiGraphics;
    }

    public static void drawString(String text, int x, int y, int color, boolean withShadow) {
        drawString(text, x, y, color, withShadow, false);
    }

    public static void drawString(String text, int x, int y, int color, boolean withShadow, boolean centered) {
        if (centered) x -= client.font.width(text) / 2;
        //#if MC > 11904
        guiGraphics.drawString(client.font, text, x, y, color, withShadow);
        //#else
        //$$ if (poseStack == null)
        //$$     throw new NullPointerException("PoseStack is null");
        //$$ if (withShadow)
        //$$     client.font.drawShadow(poseStack, text, x, y, color);
        //$$ else client.font.draw(poseStack, text, x, y, color);
        //#endif
    }

    public static Component getName(String key) {
        //#if MC > 11802
        return Component.translatable(LitematicaPrinterMod.MOD_ID + ".config.name." + key);
        //#else
        //$$ return new TranslatableComponent(LitematicaPrinterMod.MOD_ID + ".config.name." + key);
        //#endif
    }

    public static Component getComment(String key) {
        //#if MC > 11802
        return Component.translatable(LitematicaPrinterMod.MOD_ID + ".config.comment." + key);
        //#else
        //$$ return new TranslatableComponent(LitematicaPrinterMod.MOD_ID + ".config.comment." + key);
        //#endif
    }

    public static Component get(String key) {
        //#if MC > 11802
        return Component.translatable(LitematicaPrinterMod.MOD_ID + "." + key);
        //#else
        //$$ return new TranslatableComponent(LitematicaPrinterMod.MOD_ID + "." + key);
        //#endif
    }
}
