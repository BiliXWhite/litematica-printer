package me.aleksilassila.litematica.printer.interfaces;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public interface IClientPlayerInteractionManager {
    void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec);
}