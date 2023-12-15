package me.aleksilassila.litematica.printer.printer.zxy;

import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;

public class Statistics {
    //阻止UI显示 如果此时已经在UI中 请设置为2因为关闭UI也会调用一次
    public static int closeScreen = 0;
}
