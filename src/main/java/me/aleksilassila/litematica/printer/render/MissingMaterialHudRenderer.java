package me.aleksilassila.litematica.printer.render;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//#if MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import net.minecraft.client.gui.GuiComponent;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.locale.Language;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.util.GuiUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.MissingMaterialTracker;

public class MissingMaterialHudRenderer implements IInfoHudRenderer
{
    public static final MissingMaterialHudRenderer INSTANCE = new MissingMaterialHudRenderer();

    private static final int LINE_HEIGHT = 16;
    private static final int MAX_DISPLAY_ITEMS = 10;
    private static final int BG_COLOR = 0xA0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR_GRAY = 0xFF808080;

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return false;
    }

    @Override
    public boolean getShouldRenderCustom()
    {
        if (!Configs.Core.MISSING_MATERIAL_HUD.getBooleanValue()) return false;
        return MissingMaterialTracker.getInstance().hasMissing();
    }

    @Override
    public boolean shouldRenderInGuis()
    {
        return true;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return List.of();
    }

    //#if MC >= 12106
    public int render(GuiGraphics drawContext, int xOffset, int yOffset, HudAlignment alignment)
    //#elseif MC >= 12000
    //$$ public int render(int xOffset, int yOffset, HudAlignment alignment, GuiGraphics drawContext)
    //#else
    //$$ public int render(int xOffset, int yOffset, HudAlignment alignment, PoseStack matrixStack)
    //#endif
    {
        MissingMaterialTracker tracker = MissingMaterialTracker.getInstance();
        List<MissingMaterialTracker.Entry> missing = tracker.getMissing();
        if (missing.isEmpty()) return 0;

        int totalTypes = tracker.size();
        int displayCount = Math.min(missing.size(), MAX_DISPLAY_ITEMS);
        boolean showOverflow = missing.size() > MAX_DISPLAY_ITEMS;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int bgMargin = 2;
        int contentHeight = (displayCount * LINE_HEIGHT) + LINE_HEIGHT;
        if (showOverflow) contentHeight += LINE_HEIGHT;

        int maxTextLength = 0;
        for (int i = 0; i < displayCount; i++) {
            maxTextLength = Math.max(maxTextLength, font.width(getItemName(missing.get(i))));
        }
        String title = String.format(Language.getInstance().getOrDefault("litematica-printer.hud.missing.title"), totalTypes);
        int titleWidth = font.width(title);
        maxTextLength = Math.max(maxTextLength, titleWidth);
        final int maxLineLength = maxTextLength + 20;

        int posX = xOffset + bgMargin;
        switch (alignment) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                posX = (int) (GuiUtils.getScaledWindowWidth() - maxLineLength - xOffset - bgMargin);
                break;
            case CENTER:
                posX = (int) (GuiUtils.getScaledWindowWidth() / 2.0 - maxLineLength / 2.0 - xOffset);
                break;
            default:
                break;
        }

        int posY = yOffset + bgMargin;
        {
            int scaledHeight = GuiUtils.getScaledWindowHeight();
            if (alignment == HudAlignment.BOTTOM_RIGHT || alignment == HudAlignment.BOTTOM_LEFT) {
                posY = (int) (scaledHeight - posY - contentHeight);
            } else if (alignment == HudAlignment.CENTER) {
                posY = (int) (scaledHeight / 2.0 - contentHeight / 2.0 + posY);
            }
        }

        int x1 = posX - bgMargin;
        int y1 = posY - bgMargin;
        int x2 = x1 + maxLineLength + bgMargin * 2;
        int y2 = y1 + contentHeight + bgMargin;

        //#if MC >= 12000
        drawContext.fill(x1, y1, x2, y2, BG_COLOR);

        drawContext.drawString(font, title,
                posX + 2, posY + 2, TEXT_COLOR, true);

        int itemIconX = posX;
        int itemTextX = posX + 18;
        int itemY = posY + 16;
        for (int i = 0; i < displayCount; i++) {
            MissingMaterialTracker.Entry entry = missing.get(i);
            ItemStack stack = entry.item.getDefaultInstance();

            //#if MC >= 260100
            //$$ drawContext.item(stack, itemIconX, itemY);
            //$$ drawContext.itemDecorations(font, stack, itemIconX, itemY);
            //#else
            drawContext.renderItem(stack, itemIconX, itemY);
            drawContext.renderItemDecorations(font, stack, itemIconX, itemY);
            //#endif

            String name = getItemName(entry);
            int availableWidth = maxLineLength - 20;
            if (font.width(name) > availableWidth) {
                name = font.plainSubstrByWidth(name, availableWidth - font.width("...")) + "...";
            }
            drawContext.drawString(font, name, itemTextX, itemY + 4, TEXT_COLOR, true);

            itemY += LINE_HEIGHT;
        }

        if (showOverflow) {
            String overflow = String.format(Language.getInstance().getOrDefault("litematica-printer.hud.missing.overflow"), missing.size() - MAX_DISPLAY_ITEMS);
            drawContext.drawString(font, overflow,
                    posX + 2, itemY + 4, TEXT_COLOR_GRAY, true);
        }

        return contentHeight + 4;
        //#else
        //$$ return renderOld(matrixStack, missing, totalTypes, displayCount, showOverflow,
        //$$         maxLineLength, posX, posY, contentHeight, bgMargin);
        //#endif
    }

    private static String getItemName(MissingMaterialTracker.Entry entry)
    {
        return entry.displayName != null
                ? entry.displayName.getString()
                : entry.item.getDefaultInstance().getDisplayName().getString();
    }

    //#if MC < 12000
    //$$ private static int renderOld(PoseStack matrixStack,
    //$$                               List<MissingMaterialTracker.Entry> missing,
    //$$                               int totalTypes, int displayCount, boolean showOverflow,
    //$$                               int maxLineLength, int posX, int posY,
    //$$                               int contentHeight, int bgMargin)
    //$$ {
    //$$     Minecraft mc = Minecraft.getInstance();
    //$$     Font font = mc.font;
    //$$
    //$$     int x1 = posX - bgMargin;
    //$$     int y1 = posY - bgMargin;
    //$$     int x2 = x1 + maxLineLength + bgMargin * 2;
    //$$     int y2 = y1 + contentHeight + bgMargin;
    //$$
    //$$     GuiComponent.fill(matrixStack, x1, y1, x2, y2, BG_COLOR);
    //$$
    //$$     String title = String.format(Language.getInstance().getOrDefault("litematica-printer.hud.missing.title"), totalTypes);
    //$$     GuiComponent.drawString(matrixStack, font, title,
    //$$             posX + 2, posY + 2, TEXT_COLOR);
    //$$
    //$$     int itemIconX = posX;
    //$$     int itemTextX = posX + 18;
    //$$     int itemY = posY + 16;
    //$$     for (int i = 0; i < displayCount; i++) {
    //$$         MissingMaterialTracker.Entry entry = missing.get(i);
    //$$         ItemStack stack = entry.item.getDefaultInstance();
    //$$
    //$$         //#if MC >= 11900
    //$$         mc.getItemRenderer().renderGuiItem(matrixStack, stack, itemIconX, itemY);
    //$$         mc.getItemRenderer().renderGuiItemDecorations(matrixStack, font, stack, itemIconX, itemY);
    //$$         //#else
    //$$         //$$ mc.getItemRenderer().renderGuiItem(stack, itemIconX, itemY);
    //$$         //$$ mc.getItemRenderer().renderGuiItemDecorations(font, stack, itemIconX, itemY);
    //$$         //#endif
    //$$
    //$$         String name = getItemName(entry);
    //$$         int availableWidth = maxLineLength - 20;
    //$$         if (font.width(name) > availableWidth) {
    //$$             name = font.plainSubstrByWidth(name, availableWidth - font.width("...")) + "...";
    //$$         }
    //$$         GuiComponent.drawString(matrixStack, font, name, itemTextX, itemY + 4, TEXT_COLOR);
    //$$
    //$$         itemY += LINE_HEIGHT;
    //$$     }
    //$$
    //$$     if (showOverflow) {
    //$$         String overflow = String.format(Language.getInstance().getOrDefault("litematica-printer.hud.missing.overflow"), missing.size() - MAX_DISPLAY_ITEMS);
    //$$         GuiComponent.drawString(matrixStack, font, overflow,
    //$$                 posX + 2, itemY + 4, TEXT_COLOR_GRAY);
    //$$     }
    //$$
    //$$     return contentHeight + 4;
    //$$ }
    //#endif
}
