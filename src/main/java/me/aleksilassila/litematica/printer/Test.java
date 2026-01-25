package me.aleksilassila.litematica.printer;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ServiceLoader;

public class Test {
    public static void callViaSPI() {
        if (FabricLoader.getInstance().isModLoaded("tweakeroo")) {
            ServiceLoader.load(fi.dy.masa.tweakeroo.config.FeatureToggle.class).forEach(api -> {
                System.out.println(api.TWEAK_TOOL_SWITCH.getBooleanValue());
                System.out.println(fi.dy.masa.tweakeroo.config.FeatureToggle.TWEAK_TOOL_SWITCH.getBooleanValue());
            });
        }
    }
}
