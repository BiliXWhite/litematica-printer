package me.aleksilassila.litematica.printer.utils.mods;

import net.fabricmc.loader.api.FabricLoader;

public class ModLoadUtils {
    //阻止UI显示 如果此时已经在UI中 请设置为2因为关闭UI也会调用一次
    public static int closeScreen = 0;

    public static boolean isLoadMod(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isChestTrackerLoaded(){
        return isLoadMod("chesttracker");
    }

    public static boolean isQuickShulkerLoaded(){
        return isLoadMod("quickshulker");
    }

    public static boolean isBedrockMinerLoaded() {
        //#if MC >= 11900
        return isLoadMod("bedrockminer");
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean isBlockMinerLoaded() {
        //#if MC >= 11605
        return isLoadMod("blockminer");
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean isTweakerooLoaded() {
        return isLoadMod("tweakeroo");
    }
}