package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.MyBox;
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
            General.AUTO_INVENTORY.setBooleanValue(false);  // 自动设置远程交互
            General.CLOUD_INVENTORY.setBooleanValue(false); // 远程交互容器
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
                Configs.Excavate.MINE.setBooleanValue(false);
                Configs.Hotkeys.FLUID.setBooleanValue(false);
                Configs.General.PRINT_SWITCH.setBooleanValue(false);
                Configs.General.PRINTER_MODE.setOptionListValue(PrintModeType.PRINTER);
                MessageUtils.setOverlayMessage(StringUtils.nullToEmpty("已关闭全部模式"));
            }
            return true;
        });

        Hotkeys.PRINT.getKeybind().setCallback((action, key) -> {
            if (key.isKeybindHeld()) {
                if (!General.PRINT_SWITCH.getBooleanValue()) {
                    General.PRINT_SWITCH.setBooleanValue(true);
                }
            } else {
                General.PRINT_SWITCH.setBooleanValue(false);
            }
            return true;
        });

        General.PRINT_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                Printer printer = Printer.getInstance();
                printer.clearQueue();
                MyBox.resetIterations();
                printer.pistonNeedFix = false;
                printer.blockContext = null;
                if (ModLoadStatus.isBedrockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        // 切换模式时, 关闭破基岩
        General.PRINTER_MODE.setValueChangeCallback(b -> {
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
