package me.aleksilassila.litematica.printer.interfaces;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public interface IClientPlayerInteractionManager {
    public void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec);
    public ItemStack windowClick_PICKUP(int slot);

    public ItemStack windowClick_QUICK_MOVE(int slot);
}