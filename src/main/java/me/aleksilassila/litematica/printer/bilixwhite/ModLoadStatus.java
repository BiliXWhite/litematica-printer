package me.aleksilassila.litematica.printer.bilixwhite;

import net.fabricmc.loader.api.FabricLoader;

public class ModLoadStatus {
    public static boolean isTweakerooLoaded() {
        return FabricLoader.getInstance().isModLoaded("tweakeroo");
    }

    public static boolean isBedrockminerLoaded() {
        return FabricLoader.getInstance().isModLoaded("bedrockminer");
    }
}
