package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.utils.BlockUtils;
import net.minecraft.core.Direction;

public record PlayerLook(float yaw, float pitch) {

    public PlayerLook(Direction lookDirection) {
        this(BlockUtils.getRequiredYaw(lookDirection), BlockUtils.getRequiredPitch(lookDirection));
    }

    public PlayerLook(Direction lookDirectionYaw, Direction lookDirectionPitch) {
        this(BlockUtils.getRequiredYaw(lookDirectionYaw), BlockUtils.getRequiredPitch(lookDirectionPitch));
    }

    public PlayerLook(int rotation) {
        this(BlockUtils.rotationToPlayerYaw(rotation), 0);
    }
}
