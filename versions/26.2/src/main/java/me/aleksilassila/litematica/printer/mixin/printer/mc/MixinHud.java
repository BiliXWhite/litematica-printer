package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.handler.ModuleManager;
import me.aleksilassila.litematica.printer.utils.RenderUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ScanState;
import me.aleksilassila.litematica.printer.handler.Module;
import me.aleksilassila.litematica.printer.handler.GuiBlockInfo;
import me.aleksilassila.litematica.printer.handler.handlers.GUI;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mixin(Hud.class)
public abstract class MixinHud {
    @Unique
    private static final String KEY_HANDLER_TYPE = "litematica-printer.hud.debug.handlerType";
    @Unique
    private static final String KEY_SCAN_STATE = "litematica-printer.hud.debug.scanState";
    @Unique
    private static final String KEY_CURRENT_POS = "litematica-printer.hud.debug.currentPos";
    @Unique
    private static final String KEY_SCHEMATIC_BLOCK = "litematica-printer.hud.debug.schematicBlock";
    @Unique
    private static final String KEY_CURRENT_BLOCK = "litematica-printer.hud.debug.currentBlock";
    @Unique
    private static final String KEY_INTERACTED = "litematica-printer.hud.debug.interacted";
    @Unique
    private static final String KEY_IN_SELECTION = "litematica-printer.hud.debug.inSelection";
    @Unique
    private static final String KEY_EXECUTED = "litematica-printer.hud.debug.executed";
    @Unique
    private static final String KEY_GLOBAL_TICK = "litematica-printer.hud.debug.globalTick";
    @Unique
    private static final String KEY_ACTIVE_MODULES = "litematica-printer.hud.debug.activeModules";
    @Unique
    private static final String KEY_SCAN_MODE = "litematica-printer.hud.debug.scanMode";
    @Unique
    private static final String KEY_LAG_PAUSED = "litematica-printer.hud.lagPaused";

    @Unique
    private static final int DEBUG_PADDING = 4;
    @Unique
    private static final int DEBUG_LINE_HEIGHT = 12;
    @Unique
    private static final int MIN_COLUMN_WIDTH = 120;
    @Unique
    private static final int SIDE_MARGIN = 10;
    @Unique
    private static final int COLUMN_SPACING = DEBUG_PADDING * 3;
    @Unique
    private static final int COMMON_INFO_OFFSET_Y = 10;

    @Unique
    private static String booleanToColoredString(boolean value) {
        return value ? "§atrue" : "§cfalse";
    }

    @Unique
    private List<String> buildHandlerDebugLines(Module module, GuiBlockInfo guiInfo) {
        List<String> lines = new ArrayList<>();
        lines.add(MessageUtils.translatable(KEY_HANDLER_TYPE, module.getId()).getString());
        ScanState state = module.getScanState();
        lines.add(MessageUtils.translatable(KEY_SCAN_STATE, state).getString());
        lines.add(MessageUtils.translatable(KEY_CURRENT_POS, guiInfo.pos.toShortString()).getString());
        if (guiInfo.requiredState != null) {
            lines.add(MessageUtils.translatable(KEY_SCHEMATIC_BLOCK, guiInfo.requiredState.getBlock().getName().getString()).getString());
        }
        lines.add(MessageUtils.translatable(KEY_CURRENT_BLOCK, guiInfo.currentState.getBlock().getName().getString()).getString());
        lines.add(MessageUtils.translatable(KEY_INTERACTED, booleanToColoredString(guiInfo.interacted)).getString());
        lines.add(MessageUtils.translatable(KEY_IN_SELECTION, booleanToColoredString(guiInfo.posInSelectionRange)).getString());
        lines.add(MessageUtils.translatable(KEY_EXECUTED, booleanToColoredString(guiInfo.execute)).getString());

        return lines;
    }

    @Unique
    private void drawDebugLine(String text, int x, int y) {
        RenderUtils.drawString(text, x, y, new Color(0, 255, 255, 255), true);
    }

    // @formatter:off
    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    private void hookRenderItemHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator() || !ConfigUtils.isPrinterEnable()) {
            return;
        }

        float scaledWidth = mc.getWindow().getGuiScaledWidth();
        float scaledHeight = mc.getWindow().getGuiScaledHeight();

        RenderUtils.initGuiGraphics(graphics);

        if (Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
            drawDebugInfo(scaledWidth, scaledHeight);
        }

        if (Configs.Core.RENDER_HUD.getBooleanValue()) {
            drawHudInfo(scaledWidth, scaledHeight);
        }
    }
    // @formatter:on

    @Unique
    private void drawDebugInfo(float scaledWidth, float scaledHeight) {
        Minecraft mc = Minecraft.getInstance();
        List<Module> validModules = new ArrayList<>();
        Map<Module, GuiBlockInfo> guiInfoMap = new HashMap<>();
        int globalMaxTextWidth = MIN_COLUMN_WIDTH;

        for (Module module : ModuleManager.VALUES) {
            GuiBlockInfo guiInfo = module.getGuiInfo();
            if (guiInfo == null) continue;

            validModules.add(module);
            guiInfoMap.put(module, guiInfo);
            List<String> lines = buildHandlerDebugLines(module, guiInfo);
            for (String line : lines) {
                String cleanLine = line.replaceAll("§[0-9a-fA-Fklmnor]", "");
                globalMaxTextWidth = Math.max(globalMaxTextWidth, mc.font.width(cleanLine));
            }
        }

        if (validModules.isEmpty()) return;

        int commonInfoBottomY = drawCommonDebugInfo(SIDE_MARGIN, SIDE_MARGIN);

        int columnWidth = globalMaxTextWidth + DEBUG_PADDING * 2;
        int maxColumnsPerSide = calculateMaxColumnsPerSide(scaledWidth, columnWidth);
        int availableHeight = (int) (scaledHeight - commonInfoBottomY - COMMON_INFO_OFFSET_Y - SIDE_MARGIN);

        int drawnModules = drawModulePanels(
                validModules, guiInfoMap, 0,
                SIDE_MARGIN, commonInfoBottomY + COMMON_INFO_OFFSET_Y,
                columnWidth, maxColumnsPerSide, availableHeight,
                scaledHeight
        );

        if (drawnModules < validModules.size()) {
            int rightStartX = (int) (scaledWidth - SIDE_MARGIN - columnWidth);
            drawModulePanels(
                    validModules, guiInfoMap, drawnModules,
                    rightStartX, commonInfoBottomY + COMMON_INFO_OFFSET_Y,
                    columnWidth, maxColumnsPerSide, availableHeight,
                    scaledHeight
            );
        }
    }

    @Unique
    private int calculateMaxColumnsPerSide(float scaledWidth, int columnWidth) {
        float centerAreaWidth = scaledWidth * 0.5f;
        float sideAvailableWidth = (scaledWidth - centerAreaWidth) / 2 - SIDE_MARGIN * 2;

        int maxColumns = Math.max(1, (int) (sideAvailableWidth / (columnWidth + COLUMN_SPACING)));
        return Math.min(maxColumns, 3);
    }

    @Unique
    private int drawModulePanels(List<Module> modules, Map<Module, GuiBlockInfo> guiInfoMap, int startIndex,
                                  int startX, int startY, int columnWidth,
                                  int maxColumns, int availableHeight, float scaledHeight) {
        int drawnCount = 0;
        int currentColumn = 0;
        int currentX = startX;
        int currentY = startY;

        for (int i = startIndex; i < modules.size(); i++) {
            Module module = modules.get(i);
            GuiBlockInfo guiInfo = guiInfoMap.get(module);
            if (guiInfo == null) continue;

            List<String> debugLines = buildHandlerDebugLines(module, guiInfo);
            int panelHeight = debugLines.size() * DEBUG_LINE_HEIGHT + DEBUG_PADDING * 2;

            if (currentColumn >= maxColumns) {
                currentColumn = 0;
                currentX = startX;
                currentY += panelHeight + DEBUG_PADDING * 2;

                if (currentY + panelHeight > scaledHeight - SIDE_MARGIN) {
                    break;
                }
            }

            RenderUtils.fill(
                    currentX, currentY,
                    currentX + columnWidth, currentY + panelHeight,
                    new Color(0, 0, 0, 50)
            );

            int lineY = currentY + DEBUG_PADDING;
            for (String line : debugLines) {
                drawDebugLine(line, currentX + DEBUG_PADDING, lineY);
                lineY += DEBUG_LINE_HEIGHT;
            }

            drawnCount++;
            currentColumn++;
            currentX += columnWidth + COLUMN_SPACING;

            if (currentY + panelHeight > scaledHeight - SIDE_MARGIN) {
                break;
            }
        }

        return drawnCount;
    }

    @Unique
    private int drawCommonDebugInfo(int startX, int startY) {
        List<String> commonLines = new ArrayList<>();
        commonLines.add(MessageUtils.translatable(KEY_GLOBAL_TICK, ModuleManager.getCurrentHandlerTime()).getString());
        commonLines.add(MessageUtils.translatable(KEY_ACTIVE_MODULES, ModuleManager.VALUES.size()).getString());

        ScanState currentState = ScanState.RUNNING;
        for (Module m : ModuleManager.VALUES) {
            currentState = m.getScanState();
        }
        commonLines.add(MessageUtils.translatable(KEY_SCAN_MODE, currentState).getString());

        Minecraft mc = Minecraft.getInstance();
        int maxWidth = 0;
        for (String line : commonLines) {
            String cleanLine = line.replaceAll("§[0-9a-fA-Fklmnor]", "");
            maxWidth = Math.max(maxWidth, mc.font.width(cleanLine));
        }

        int bgWidth = maxWidth + DEBUG_PADDING * 2;
        int bgHeight = commonLines.size() * DEBUG_LINE_HEIGHT + DEBUG_PADDING * 2;

        RenderUtils.fill(
                startX, startY,
                startX + bgWidth, startY + bgHeight,
                new Color(0, 0, 0, 50)
        );

        int lineY = startY + DEBUG_PADDING;
        for (String line : commonLines) {
            drawDebugLine(line, startX + DEBUG_PADDING, lineY);
            lineY += DEBUG_LINE_HEIGHT;
        }

        return startY + bgHeight;
    }

    @Unique
    private void drawHudInfo(float scaledWidth, float scaledHeight) {
        int centerX = (int) (scaledWidth / 2);
        int centerY = (int) (scaledHeight / 2);
        GUI guiHandler = ModuleManager.GUI;

        if (Configs.Core.LAG_CHECK.getBooleanValue() && ModuleManager.getPacketTick() > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
            RenderUtils.drawString(MessageUtils.translatable(KEY_LAG_PAUSED).getString(), centerX, centerY - 22, Color.ORANGE, true, true);
        }

        double progress = guiHandler.getTotalProgress().getProgress();
        RenderUtils.drawString((int) (progress * 100) + "%", centerX, centerY + 22, Color.WHITE, true, true);
        drawProgressBar(centerX, centerY + 36, 40, 6, progress, new Color(0, 0, 0, 150), new Color(0, 255, 0, 255));

        int infoY = centerY + 52;

        HashSet<String> modeNames = new HashSet<>();
        for (Module module : ModuleManager.VALUES) {
            if (module.getId().equals(GUI.NAME) || module.getEnableConfig() == null || !module.getEnableConfig().getBooleanValue()) {
                continue;
            }
            modeNames.add(module.getEnableConfig().getPrettyName());
        }
        RenderUtils.drawString(String.join(", ", modeNames), centerX, infoY, Color.WHITE, true, true);
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