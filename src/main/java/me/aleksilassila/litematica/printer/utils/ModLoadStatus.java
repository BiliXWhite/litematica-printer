package me.aleksilassila.litematica.printer.utils;

import net.fabricmc.loader.api.FabricLoader;

public class ModLoadStatus {
    // 阻止 UI 显示 如果此时已经在 UI 中 请设置为 2 因为关闭 UI 也会调用一次
    public static int closeScreen = 0;

    public static boolean isLoadMod(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isLoadChestTrackerLoaded(){
        return isLoadMod("chesttracker");
    }

    public static boolean isLoadQuickShulkerLoaded(){
        return isLoadMod("quickshulker");
    }

    public static boolean isBedrockMinerLoaded() {
        //#if MC >= 11900
        return isLoadMod("bedrockminer");
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean isTweakerooLoaded() {
        return isLoadMod("tweakeroo");
    }
}