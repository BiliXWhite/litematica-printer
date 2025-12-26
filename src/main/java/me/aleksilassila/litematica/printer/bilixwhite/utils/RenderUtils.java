package me.aleksilassila.litematica.printer.bilixwhite.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

// 这应该是渲染工具而不是字符串工具
public class RenderUtils {
    public static final Minecraft client = Minecraft.getInstance();
    private static PoseStack poseStack;
    private static GuiGraphics guiGraphics;

    public static void initMatrix(PoseStack poseStack) {
        RenderUtils.poseStack = poseStack;
    }

    public static void initGuiGraphics(GuiGraphics guiGraphics) {
        RenderUtils.guiGraphics = guiGraphics;
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
}
