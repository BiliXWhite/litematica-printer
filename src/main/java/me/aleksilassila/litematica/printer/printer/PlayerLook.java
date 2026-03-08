package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import net.minecraft.core.Direction;

public record PlayerLook(float yaw, float pitch) {

    public PlayerLook(Direction lookDirection) {
        this(DirectionUtils.getRequiredYaw(lookDirection), DirectionUtils.getRequiredPitch(lookDirection));
    }

    public PlayerLook(Direction lookDirectionYaw, Direction lookDirectionPitch) {
        this(DirectionUtils.getRequiredYaw(lookDirectionYaw), DirectionUtils.getRequiredPitch(lookDirectionPitch));
    }

    public PlayerLook(int rotation) {
        this(DirectionUtils.rotationToPlayerYaw(rotation), 0);
    }
}
