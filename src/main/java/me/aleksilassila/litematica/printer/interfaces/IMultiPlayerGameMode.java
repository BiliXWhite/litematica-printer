package me.aleksilassila.litematica.printer.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public interface IMultiPlayerGameMode {
    void rightClickBlock(BlockPos pos, Direction side, Vec3 hitVec);
}