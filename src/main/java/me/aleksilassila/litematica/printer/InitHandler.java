package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.HighlightBlockRenderer;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.utils.StringUtils;

import static me.aleksilassila.litematica.printer.config.Configs.*;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        Configs.init();
        initModConfig();
        initConfigCallback();
        HighlightBlockRenderer.init();  // 高亮显示方块渲染器
    }

    private static void initModConfig() {
        // 箱子追踪(模组没加载的情况下, 进行关闭)
        if (!ModLoadStatus.isLoadChestTrackerLoaded()) {
            Core.AUTO_INVENTORY.setBooleanValue(false);  // 自动设置远程交互
            Core.CLOUD_INVENTORY.setBooleanValue(false); // 远程交互容器
        }
        //#if MC >= 12001 && MC <= 12104
        //$$ if (ModLoadStatus.isLoadChestTrackerLoaded()) {
        //$$     me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.setup();
        //$$ }
        //#endif
    }

    private void initConfigCallback() {
        Hotkeys.CLOSE_ALL_MODE.getKeybind().setCallback((action, keybind) -> {
            if (keybind.isKeybindHeld()) {
                Core.MINE.setBooleanValue(false);
                Core.FLUID.setBooleanValue(false);
                Core.WORK_SWITCH.setBooleanValue(false);
                Core.WORK_MODE_TYPE.setOptionListValue(PrintModeType.PRINTER);
                MessageUtils.setOverlayMessage(StringUtils.nullToEmpty("已关闭全部模式"));
            }
            return true;
        });

        Core.WORK_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                Printer printer = Printer.getInstance();
                printer.clearQueue();
                printer.pistonNeedFix = false;
                if (ModLoadStatus.isBedrockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        // 切换模式时, 关闭破基岩
        Core.WORK_MODE_TYPE.setValueChangeCallback(b -> {
            if (!b.getOptionListValue().equals(PrintModeType.BEDROCK)) {
                if (ModLoadStatus.isBedrockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });
    }
}
