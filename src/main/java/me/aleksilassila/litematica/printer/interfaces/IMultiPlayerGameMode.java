package me.aleksilassila.litematica.printer.interfaces;

import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;

public interface IMultiPlayerGameMode {
    void litematica_printer$rightClickBlock(BlockPos pos, Direction side, Vec3 hitVec);
}