package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class Statistics {
    //阻止UI显示 如果此时已经在UI中 请设置为2因为关闭UI也会调用一次
    public static int closeScreen = 0;
    //箱子追踪点击的物品
    public static ItemStack itemStack = null;
    //箱子追踪当前打开的维度
    public static Identifier currentMemoryKey = null;
    //远程取物返回包中的方块数据
    public static BlockState blockState = null;
}
