package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.utils.RenderUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.WorkingModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.GuiBlockInfo;
import me.aleksilassila.litematica.printer.handler.Handlers;
import me.aleksilassila.litematica.printer.handler.handlers.GuiHandler;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

//#if MC <= 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//$$import com.mojang.blaze3d.vertex.PoseStack;
//$$import org.lwjgl.opengl.GL11;
//$$import org.lwjgl.opengl.GL11C;
//#elseif MC > 12006
import net.minecraft.client.DeltaTracker;
//#endif

/**
 * HUD渲染Mixin，负责打印器调试信息和进度条的绘制
 */
@SuppressWarnings({"SameParameterValue", "SpellCheckingInspection"})
@Mixin(Gui.class)
public abstract class MixinGui {
    // @formatter:off

    @Unique
    private static final int DEBUG_ROOT_X = 10;
    @Unique
    private static final int DEBUG_ROOT_Y = 10;
    @Unique
    private static final int DEBUG_COLUMN_WIDTH = 150;
    @Unique
    private static final int DEBUG_LINE_HEIGHT = 12;
    @Unique
    private static final int DEBUG_PADDING = 4;
    @Unique
    private static final int MAX_DEBUG_COLUMNS = 3;

    // @formatter:on

    @SuppressWarnings("SpellCheckingInspection")
    @Unique
    private static String booleanToColoredString(boolean value) {
        return value ? "§atrue" : "§cfalse";
    }

    // @formatter:off
    @Inject(method = "renderItemHotbar", at = @At("TAIL"))
    //#if MC > 12006
    private void hookRenderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci
    //#elseif MC >= 12006
    //$$ private void hookRenderItemHotbar(GuiGraphics guiGraphics, float f, CallbackInfo ci
    //#elseif MC > 11904 && MC < 12006
    //$$ private void hookRenderItemHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci
    //#else
    //$$ private void hookRenderItemHotbar(float f, PoseStack poseStack, CallbackInfo ci
    //#endif
    ) {
        // 前置条件校验：快速失败
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isSpectator()) return;
        if (!ConfigUtils.isEnable()) return;

        float scaledWidth = mc.getWindow().getGuiScaledWidth();
        float scaledHeight = mc.getWindow().getGuiScaledHeight();

        // 初始化渲染矩阵
        //#if MC <= 11904
        //$$ RenderUtils.initMatrix(poseStack);
        //#else
        RenderUtils.initGuiGraphics(guiGraphics);
        //#endif

        // 绘制调试信息
        if (Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
            drawSmoothDebugInfo(scaledWidth, scaledHeight);
        }

        // 绘制HUD
        if (Configs.Core.RENDER_HUD.getBooleanValue()) {
            drawHudInfo(scaledWidth, scaledHeight);
        }
    }
    // @formatter:on

    /**
     * 平滑绘制DEBUG信息（保持原有逻辑）
     */
    @Unique
    private void drawSmoothDebugInfo(float scaledWidth, float scaledHeight) {
        int currentColumn = 0;
        int currentX = DEBUG_ROOT_X;
        int currentY = DEBUG_ROOT_Y;
        int maxColumnWidth = DEBUG_COLUMN_WIDTH;
        int maxLineHeightPerBlock = DEBUG_LINE_HEIGHT * 8 + DEBUG_PADDING * 2;

        for (ClientPlayerTickHandler handler : Handlers.VALUES) {
            GuiBlockInfo guiInfo = handler.getCurrentRenderGuiBlockInfo();
            if (guiInfo == null) continue;

            if (currentColumn >= MAX_DEBUG_COLUMNS || (currentX + maxColumnWidth > scaledWidth - 10)) {
                currentColumn = 0;
                currentX = DEBUG_ROOT_X;
                currentY += maxLineHeightPerBlock + DEBUG_PADDING * 2;
                if (currentY + maxLineHeightPerBlock > scaledHeight - 10) break;
            }

            // 绘制半透背景
            int bgX1 = currentX - DEBUG_PADDING;
            int bgY1 = currentY - DEBUG_PADDING;
            int bgX2 = currentX + maxColumnWidth + DEBUG_PADDING;
            int bgY2 = currentY + maxLineHeightPerBlock + DEBUG_PADDING;
            RenderUtils.fill(bgX1, bgY1, bgX2, bgY2, new Color(0, 0, 0, 120));

            // 数据防抖
            int currentRenderIndex = handler.getRenderIndex();
            int currentQueueSize = handler.getGuiBlockInfoQueueSize();

            // 绘制调试文本
            Block block = guiInfo.state.getBlock();
            BlockPos blockPos = guiInfo.pos;
            MutableComponent blockName = block.getName();

            int lineY = currentY;
            drawDebugLine("Tick: " + ClientPlayerTickHandler.getCurrentHandlerTime(), currentX + DEBUG_PADDING, currentY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("类型: " + handler.getId(), currentX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("位置: " + blockPos.toShortString(), currentX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("方块: " + blockName.getString(), currentX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("交互: " + booleanToColoredString(guiInfo.interacted), currentX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("范围: " + booleanToColoredString(guiInfo.posInSelectionRange), currentX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("执行: " + booleanToColoredString(guiInfo.execute), currentX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;

            drawDebugLine("HUD迭代(同刻): " + currentRenderIndex + "/" + currentQueueSize, currentX + DEBUG_PADDING, lineY);

            currentColumn++;
            currentX += maxColumnWidth + DEBUG_PADDING * 2;
        }
    }

    @Unique
    private void drawDebugLine(String text, int x, int y) {
        RenderUtils.drawString(text, x, y, new Color(0, 255, 255, 255), true);
    }

    @Unique
    private void drawHudInfo(float scaledWidth, float scaledHeight) {
        int centerX = (int) (scaledWidth / 2);
        int centerY = (int) (scaledHeight / 2);
        GuiHandler guiHandler = Handlers.GUI;

        // 绘制延迟过大警告（保持不变）
        if (Configs.Core.LAG_CHECK.getBooleanValue() && ClientPlayerTickHandler.getPacketTick() > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
            RenderUtils.drawString(
                    "延迟过大，已暂停运行",
                    centerX,
                    centerY - 22,
                    Color.ORANGE,
                    true,
                    true
            );
        }

        WorkingModeType workMode = (WorkingModeType) Configs.Core.WORK_MODE.getOptionListValue();
        if (workMode.equals(WorkingModeType.SINGLE)) {
            double progress = guiHandler.getTotalProgress().getProgress();
            String progressString = (int) (progress * 100) + "%";
            RenderUtils.drawString(progressString, centerX, centerY + 22, Color.WHITE, true, true);
            drawProgressBar(centerX, centerY + 36, 40, 6, progress, new Color(0, 0, 0, 150), new Color(0, 255, 0, 255));
        }
        String modeName = Configs.Core.WORK_MODE_TYPE.getOptionListValue().getDisplayName();
        RenderUtils.drawString(modeName, centerX, centerY + 52, Color.WHITE, true, true);
    }


    @Unique
    private void drawProgressBar(int x, int y, int barWidth, int barHeight, double progress, Color bgColor, Color fgColor) {
        double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
        int barXStart = x - (barWidth / 2); // 居中：x是中心点，左边界=中心-宽/2
        int barXEnd = x + (barWidth / 2);   // 右边界=中心+宽/2
        // 进度条背景Y坐标（顶部y，底部y+高度）
        int barYEnd = y + barHeight;
        int filledWidth = (int) (clampedProgress * barWidth);
        int fgXEnd = barXStart + filledWidth; // 前景右边界
        RenderUtils.fill(barXStart, y, barXEnd, barYEnd, bgColor);
        if (filledWidth > 0) {
            RenderUtils.fill(barXStart, y, fgXEnd, barYEnd, fgColor);
        }
    }
}
