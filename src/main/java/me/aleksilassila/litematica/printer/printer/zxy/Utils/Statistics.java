package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import net.fabricmc.loader.api.FabricLoader;

public class Statistics {
    //阻止UI显示 如果此时已经在UI中 请设置为2因为关闭UI也会调用一次
    public static int closeScreen = 0;
    public static boolean loadChestTracker = isLoadMod("chesttracker");
    public static boolean loadQuickShulker = isLoadMod("quickshulker");
    public static boolean loadCarpetWuHuAddition = isLoadMod("carpet-wuhu-addition");
    public static boolean isLoadMod(String modId){
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
