package me.aleksilassila.litematica.printer.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import me.aleksilassila.litematica.printer.handler.Handlers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

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

    public static void ensureInitialized() {
        //#if MC > 11904
        if (guiGraphics == null) {
            throw new NullPointerException("GuiGraphics is null! Call initGuiGraphics first.");
        }
        //#else
        //$$ if (poseStack == null) {
        //$$     throw new NullPointerException("PoseStack is null! Call initMatrix first.");
        //$$ }
        //#endif
    }

    public static void drawString(String text, int x, int y, Color color, boolean withShadow) {
        drawString(text, x, y, color, withShadow, false);
    }

    public static void drawString(String text, int x, int y, Color color, boolean withShadow, boolean centered) {
        if (centered) {
            x -= client.font.width(text) / 2;
        }
        ensureInitialized();
        //#if MC > 11904
        guiGraphics.drawString(client.font, text, x, y, color.getRGB(), withShadow);
        //#else
        //$$ if (withShadow) {
        //$$    client.font.drawShadow(poseStack, text, x, y, color.getRGB());
        //$$ } else {
        //$$    client.font.draw(poseStack, text, x, y, color.getRGB());
        //$$ }
        //#endif
    }

    public static void fill(int x1, int y1, int x2, int y2, Color color) {
        ensureInitialized();
        //#if MC > 11904
        guiGraphics.fill(x1, y1, x2, y2, color.getRGB());
        //#else
        //$$ GuiComponent.fill(poseStack, x1, y1, x2, y2, color.getRGB());
        //#endif
    }
}
