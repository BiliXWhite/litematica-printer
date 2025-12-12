package me.aleksilassila.litematica.printer.bilixwhite;

import net.fabricmc.loader.api.FabricLoader;

public class ModLoadStatus {
    //阻止UI显示 如果此时已经在UI中 请设置为2因为关闭UI也会调用一次
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
