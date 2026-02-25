package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import me.aleksilassila.litematica.printer.utils.RenderUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.WorkingModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.GuiBlockInfo;
import me.aleksilassila.litematica.printer.handler.handlers.GuiHandler;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

//#if MC <= 11904
//$$import com.mojang.blaze3d.vertex.PoseStack;
//#elseif MC > 12006
import net.minecraft.client.DeltaTracker;
//#endif

/**
 * HUD渲染Mixin，负责打印器调试信息和进度条的绘制
 */
@SuppressWarnings({"SameParameterValue", "SpellCheckingInspection"})
@Mixin(Gui.class)
public abstract class MixinGui {
    @Unique
    private static final int DEBUG_PADDING = 4;
    @Unique
    private static final int DEBUG_LINE_HEIGHT = 12;
    @Unique
    private static final int MIN_COLUMN_WIDTH = 120;
    @Unique
    private static final int SIDE_MARGIN = 10; // 屏幕左右边距
    @Unique
    private static final int COLUMN_SPACING = DEBUG_PADDING * 3; // 列之间的间距
    @Unique
    private static final int COMMON_INFO_OFFSET_Y = 10;

    @Unique
    private static String booleanToColoredString(boolean value) {
        return value ? "§atrue" : "§cfalse";
    }

    @Unique
    private static String formatAlignedNumber(int current, int total) {
        int totalDigits = total == 0 ? 1 : String.valueOf(total).length();
        DecimalFormat formatter = new DecimalFormat(String.format("%0" + totalDigits + "d", 0));
        return formatter.format(current);
    }

    @Unique
    private List<String> buildHandlerDebugLines(ClientPlayerTickHandler handler, GuiBlockInfo guiInfo) {
        List<String> lines = new ArrayList<>();
        lines.add("处理类型: " + handler.getId());
        lines.add("当前位置: " + guiInfo.pos.toShortString());
        if (guiInfo.requiredState != null) {
            lines.add("投影方块: " + guiInfo.requiredState.getBlock().getName().getString());
        }
        lines.add("当前方块: " + guiInfo.currentState.getBlock().getName().getString());
        lines.add("交互范围: " + booleanToColoredString(guiInfo.interacted));
        lines.add("选区类型: " + booleanToColoredString(guiInfo.posInSelectionRange));
        lines.add("已经执行: " + booleanToColoredString(guiInfo.execute));

        int renderIndex = handler.getRenderIndex();
        int queueSize = handler.getGuiBlockInfoQueueSize();
        lines.add("同刻迭代(GUI): " + formatAlignedNumber(renderIndex, queueSize) + "/" + queueSize);

        return lines;
    }

    @Unique
    private void drawDebugLine(String text, int x, int y) {
        RenderUtils.drawString(text, x, y, new Color(0, 255, 255, 255), true);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator() || !ConfigUtils.isEnable()) {
            return;
        }

        float scaledWidth = mc.getWindow().getGuiScaledWidth();
        float scaledHeight = mc.getWindow().getGuiScaledHeight();

        // 初始化渲染矩阵
        //#if MC <= 11904
        //$$ RenderUtils.initMatrix(poseStack);
        //#else
        RenderUtils.initGuiGraphics(guiGraphics);
        //#endif

        if (Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
            drawDebugInfo(scaledWidth, scaledHeight);
        }

        if (Configs.Core.RENDER_HUD.getBooleanValue()) {
            drawHudInfo(scaledWidth, scaledHeight);
        }
    }
    // @formatter:on

    // 调试信息绘制
    @Unique
    private void drawDebugInfo(float scaledWidth, float scaledHeight) {
        Minecraft mc = Minecraft.getInstance();
        List<ClientPlayerTickHandler> validHandlers = new ArrayList<>();
        int globalMaxTextWidth = MIN_COLUMN_WIDTH;

        // 1. 收集有效Handler并计算全局最大宽度
        for (ClientPlayerTickHandler handler : ClientPlayerTickManager.VALUES) {
            GuiBlockInfo guiInfo = handler.getCurrentRenderGuiBlockInfo();
            if (guiInfo == null) continue;

            validHandlers.add(handler);
            List<String> lines = buildHandlerDebugLines(handler, guiInfo);
            for (String line : lines) {
                String cleanLine = line.replaceAll("§[0-9a-fA-Fklmnor]", "");
                globalMaxTextWidth = Math.max(globalMaxTextWidth, mc.font.width(cleanLine));
            }
        }

        if (validHandlers.isEmpty()) return;

        // 2. 绘制公共信息（左上角）
        int commonInfoBottomY = drawCommonDebugInfo(SIDE_MARGIN, SIDE_MARGIN);

        // 3. 计算布局参数（动态适配屏幕）
        int columnWidth = globalMaxTextWidth + DEBUG_PADDING * 2;
        int maxColumnsPerSide = calculateMaxColumnsPerSide(scaledWidth, columnWidth);
        int availableHeight = (int) (scaledHeight - commonInfoBottomY - COMMON_INFO_OFFSET_Y - SIDE_MARGIN);

        // 4. 优先绘制左侧面板，再绘制右侧
        int drawnHandlers = drawHandlerPanels(
                validHandlers, 0,
                SIDE_MARGIN, commonInfoBottomY + COMMON_INFO_OFFSET_Y,
                columnWidth, maxColumnsPerSide, availableHeight,
                scaledHeight
        );

        // 如果左侧绘制不完，绘制右侧面板
        if (drawnHandlers < validHandlers.size()) {
            int rightStartX = (int) (scaledWidth - SIDE_MARGIN - columnWidth);
            drawHandlerPanels(
                    validHandlers, drawnHandlers,
                    rightStartX, commonInfoBottomY + COMMON_INFO_OFFSET_Y,
                    columnWidth, maxColumnsPerSide, availableHeight,
                    scaledHeight
            );
        }
    }

    /**
     * 计算单侧边最多能显示的列数（根据屏幕宽度动态调整）
     */
    @Unique
    private int calculateMaxColumnsPerSide(float scaledWidth, int columnWidth) {
        // 屏幕中间预留核心游戏区域（占总宽度的50%）
        float centerAreaWidth = scaledWidth * 0.5f;
        float sideAvailableWidth = (scaledWidth - centerAreaWidth) / 2 - SIDE_MARGIN * 2;

        // 计算单侧边能容纳的列数（至少1列）
        int maxColumns = Math.max(1, (int) (sideAvailableWidth / (columnWidth + COLUMN_SPACING)));
        return Math.min(maxColumns, 3); // 最多3列，避免过于拥挤
    }

    /**
     * 绘制指定范围的Handler面板
     *
     * @param startIndex 起始Handler索引
     * @return 实际绘制的Handler数量
     */
    @Unique
    private int drawHandlerPanels(List<ClientPlayerTickHandler> handlers, int startIndex,
                                  int startX, int startY, int columnWidth,
                                  int maxColumns, int availableHeight, float scaledHeight) {
        int drawnCount = 0;
        int currentColumn = 0;
        int currentX = startX;
        int currentY = startY;

        for (int i = startIndex; i < handlers.size(); i++) {
            ClientPlayerTickHandler handler = handlers.get(i);
            GuiBlockInfo guiInfo = handler.getCurrentRenderGuiBlockInfo();
            if (guiInfo == null) continue;

            // 构建调试文本并计算面板高度
            List<String> debugLines = buildHandlerDebugLines(handler, guiInfo);
            int panelHeight = debugLines.size() * DEBUG_LINE_HEIGHT + DEBUG_PADDING * 2;

            // 列数满了，换行
            if (currentColumn >= maxColumns) {
                currentColumn = 0;
                currentX = startX;
                currentY += panelHeight + DEBUG_PADDING * 2;

                // 超出屏幕高度，停止绘制
                if (currentY + panelHeight > scaledHeight - SIDE_MARGIN) {
                    break;
                }
            }

            // 绘制面板背景
            RenderUtils.fill(
                    currentX, currentY,
                    currentX + columnWidth, currentY + panelHeight,
                    new Color(0, 0, 0, 50)
            );

            // 绘制文本
            int lineY = currentY + DEBUG_PADDING;
            for (String line : debugLines) {
                drawDebugLine(line, currentX + DEBUG_PADDING, lineY);
                lineY += DEBUG_LINE_HEIGHT;
            }

            // 更新位置和计数
            drawnCount++;
            currentColumn++;
            currentX += columnWidth + COLUMN_SPACING;

            // 检查是否超出当前列的高度限制
            if (currentY + panelHeight > scaledHeight - SIDE_MARGIN) {
                break;
            }
        }

        return drawnCount;
    }

    /**
     * 绘制公共调试信息（左上角）
     */
    @Unique
    private int drawCommonDebugInfo(int startX, int startY) {
        List<String> commonLines = new ArrayList<>();
        commonLines.add("全局Tick: " + ClientPlayerTickManager.getCurrentHandlerTime());
        commonLines.add("活跃Handler数: " + ClientPlayerTickManager.VALUES.size());

        Minecraft mc = Minecraft.getInstance();
        int maxWidth = 0;
        for (String line : commonLines) {
            String cleanLine = line.replaceAll("§[0-9a-fA-Fklmnor]", "");
            maxWidth = Math.max(maxWidth, mc.font.width(cleanLine));
        }

        int bgWidth = maxWidth + DEBUG_PADDING * 2;
        int bgHeight = commonLines.size() * DEBUG_LINE_HEIGHT + DEBUG_PADDING * 2;

        // 绘制公共信息背景
        RenderUtils.fill(
                startX, startY,
                startX + bgWidth, startY + bgHeight,
                new Color(0, 0, 0, 50)
        );

        // 绘制文本
        int lineY = startY + DEBUG_PADDING;
        for (String line : commonLines) {
            drawDebugLine(line, startX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;
        }

        return startY + bgHeight;
    }

    // ========== HUD进度条等信息绘制 ==========
    @Unique
    private void drawHudInfo(float scaledWidth, float scaledHeight) {
        int centerX = (int) (scaledWidth / 2);
        int centerY = (int) (scaledHeight / 2);
        GuiHandler guiHandler = ClientPlayerTickManager.GUI;

        // 延迟过大警告
        if (Configs.Core.LAG_CHECK.getBooleanValue() && ClientPlayerTickManager.getPacketTick() > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
            RenderUtils.drawString("延迟过大，已暂停运行", centerX, centerY - 22, Color.ORANGE, true, true);
        }

        // 单模式进度条
        WorkingModeType workMode = (WorkingModeType) Configs.Core.WORK_MODE.getOptionListValue();
        if (workMode.equals(WorkingModeType.SINGLE)) {
            double progress = guiHandler.getTotalProgress().getProgress();
            RenderUtils.drawString((int) (progress * 100) + "%", centerX, centerY + 22, Color.WHITE, true, true);
            drawProgressBar(centerX, centerY + 36, 40, 6, progress, new Color(0, 0, 0, 150), new Color(0, 255, 0, 255));
        }

        // 模式名称显示
        if (ConfigUtils.isSingleMode()) {
            String modeName = Configs.Core.WORK_MODE_TYPE.getOptionListValue().getDisplayName();
            RenderUtils.drawString(modeName, centerX, centerY + 52, Color.WHITE, true, true);
        } else {
            HashSet<String> modeNames = new HashSet<>();
            for (ClientPlayerTickHandler handler : ClientPlayerTickManager.VALUES) {
                if (handler.getId().equals(GuiHandler.NAME) || handler.getEnableConfig() == null || !handler.getEnableConfig().getBooleanValue()) {
                    continue;
                }
                modeNames.add(handler.getEnableConfig().getPrettyName());
            }
            RenderUtils.drawString(String.join(", ", modeNames), centerX, centerY + 52, Color.WHITE, true, true);
        }
    }

    @Unique
    private void drawProgressBar(int x, int y, int barWidth, int barHeight, double progress, Color bgColor, Color fgColor) {
        double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
        int barXStart = x - (barWidth / 2);
        int barXEnd = x + (barWidth / 2);
        int barYEnd = y + barHeight;
        int filledWidth = (int) (clampedProgress * barWidth);

        RenderUtils.fill(barXStart, y, barXEnd, barYEnd, bgColor);
        if (filledWidth > 0) {
            RenderUtils.fill(barXStart, y, barXStart + filledWidth, barYEnd, fgColor);
        }
    }
}