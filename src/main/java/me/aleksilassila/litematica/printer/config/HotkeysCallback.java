package me.aleksilassila.litematica.printer.config;

import me.aleksilassila.litematica.printer.gui.ConfigUi;
import me.aleksilassila.litematica.printer.handler.ModuleManager;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.interfaces.compat.BedrockCompat;
import net.minecraft.client.Minecraft;

// 按键与回调注册
public class HotkeysCallback {
    private static final Minecraft client = Minecraft.getInstance();

    public static void initCallbacks() {
        // 打开设置界面
        Configs.Hotkeys.OPEN_SCREEN.getKeybind().setCallback((action, keybind) -> {
            if (client.player != null && client.level != null) {
                client.setScreen(new ConfigUi());
            }
            return true;
        });
        Configs.Hotkeys.CYCLE_MODE.getKeybind().setCallback((action, keybind) -> {
            var modes = new fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed[]{
                    Configs.Print.ENABLED,
                    Configs.Mine.ENABLED,
                    Configs.Fill.ENABLED,
                    Configs.Fluid.ENABLED,
                    Configs.Bedrock.ENABLED
            };
            int current = -1;
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].getBooleanValue()) {
                    current = i;
                    break;
                }
            }
            for (var m : modes) m.setBooleanValue(false);
            int next = (current + 1) % modes.length;
            modes[next].setBooleanValue(true);
            MessageUtils.setOverlayMessage(modes[next].getPrettyName());
            return true;
        });

        Configs.Hotkeys.CLOSE_ALL_MODE.getKeybind().setCallback((action, keybind) -> {
            if (keybind.isKeybindHeld()) {
                Configs.Print.ENABLED.setBooleanValue(false);
                Configs.Mine.ENABLED.setBooleanValue(false);
                Configs.Fill.ENABLED.setBooleanValue(false);
                Configs.Fluid.ENABLED.setBooleanValue(false);
                Configs.Bedrock.ENABLED.setBooleanValue(false);
                Configs.Core.WORK_SWITCH.setBooleanValue(false);
                MessageUtils.setOverlayMessage(MessageUtils.nullToEmpty("已关闭全部模式"));
            }
            return true;
        });

        // 工作开关
        Configs.Core.WORK_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                ModuleManager.PRINT.setWatingForWaterPos(null);
                ActionManager.INSTANCE.clearQueue();
                if (BedrockCompat.isAvailable()) {
                    if (BedrockCompat.isWorking()) {
                        BedrockCompat.setWorking(false);
                        BedrockCompat.setFeatureEnable(true);
                    }
                }
            }
        });

        // 切换基岩功能时，关闭破基岩
        Configs.Bedrock.ENABLED.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                if (BedrockCompat.isAvailable()) {
                    if (BedrockCompat.isWorking()) {
                        BedrockCompat.setWorking(false);
                        BedrockCompat.setFeatureEnable(true);
                    }
                }
            }
        });

        // 特殊设置时，自动刷新界面
        Configs.Print.FILL_COMPOSTER.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Break.BREAK_LIMITER.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Break.BREAK_LIMIT.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Mine.EXCAVATE_LIMITER.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Mine.EXCAVATE_LIMIT.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Fill.FILL_BLOCK_MODE.setValueChangeCallback(b -> ConfigUi.refresh());
        Configs.Core.LAG_CHECK.setValueChangeCallback(b -> ConfigUi.refresh());
    }
}