package me.aleksilassila.litematica.printer;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组核心常量引用类
 * 集中管理模组的全局固定值，避免硬编码和拼写错误
 */
public class Reference {
    public static final Minecraft MINECRAFT = Minecraft.getInstance();
    public static final String MOD_ID = "litematica_printer";
    public static final String MOD_NAME = "Litematica Printer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
}
