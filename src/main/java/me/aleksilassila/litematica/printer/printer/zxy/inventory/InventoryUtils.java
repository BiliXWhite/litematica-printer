package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class InventoryUtils {
    public static boolean isInventory(World world, BlockPos pos){
        return fi.dy.masa.malilib.util.InventoryUtils.getInventory(world,pos) != null;
    }
}
