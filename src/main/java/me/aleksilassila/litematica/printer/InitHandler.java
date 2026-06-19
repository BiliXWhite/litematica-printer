package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import fi.dy.masa.malilib.event.RenderEventHandler;
import me.aleksilassila.litematica.printer.render.BlockHighlightRenderer;
import me.aleksilassila.litematica.printer.render.MissingMaterialHudRenderer;
import me.aleksilassila.litematica.printer.utils.ModUtils;
import me.aleksilassila.litematica.printer.utils.bedrock.BedrockUtils;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        Configs.init();
        initConfigCallback();
        fi.dy.masa.litematica.render.infohud.InfoHud.getInstance()
                .addInfoHudRenderer(MissingMaterialHudRenderer.INSTANCE, true);

        RenderEventHandler.getInstance().registerWorldLastRenderer(new BlockHighlightRenderer());
    }

    private void initConfigCallback() {
        Configs.Hotkeys.CLOSE_ALL_MODE.getKeybind().setCallback((action, keybind) -> {
            if (keybind.isKeybindHeld()) {
                Configs.Core.MINE.setBooleanValue(false);
                Configs.Core.FLUID.setBooleanValue(false);
                Configs.Core.WORK_SWITCH.setBooleanValue(false);
                Configs.Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
                MessageUtils.setOverlayMessage(MessageUtils.nullToEmpty("已关闭全部模式"));
            }
            return true;
        });

        // 工作开关
        Configs.Core.WORK_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                ActionManager.INSTANCE.clearQueue();
                if (ModUtils.isBedrockMinerLoaded() || ModUtils.isBlockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        // 切换模式时, 关闭破基岩
        Configs.Core.WORK_MODE_TYPE.setValueChangeCallback(b -> {
            if (!b.getOptionListValue().equals(PrintModeType.BEDROCK)) {
                if (ModUtils.isBedrockMinerLoaded() || ModUtils.isBlockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        // 特殊设置时，自动刷新界面
        Configs.Core.WORK_MODE.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Print.FILL_COMPOSTER.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Break.BREAK_LIMITER.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Break.BREAK_LIMIT.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Mine.EXCAVATE_LIMITER.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Mine.EXCAVATE_LIMIT.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Fill.FILL_BLOCK_MODE.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Core.LAG_CHECK.setValueChangeCallback(b -> ConfigUi.refresh());
    }
}
