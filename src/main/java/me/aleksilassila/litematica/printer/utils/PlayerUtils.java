package me.aleksilassila.litematica.printer.utils;

import net.minecraft.client.Minecraft;

public class PlayerUtils {
    /**
     * 获取方块交互范围
     */
    public static double getBlockInteractionRange(Minecraft minecraft) {
        //#if MC>=12005
        if (minecraft.player != null) {
            return minecraft.player.blockInteractionRange();
        }
        //#else
        //$$ if (minecraft.gameMode != null) {
        //$$    return minecraft.gameMode.getPickRange();
        //$$ }
        //#endif
        return 4.5F;
    }
}
