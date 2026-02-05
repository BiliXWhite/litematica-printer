package me.aleksilassila.litematica.printer.printer;

import lombok.Data;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import net.minecraft.core.Direction;

@SuppressWarnings("ClassCanBeRecord")
@Data
public class Look {
    public final float yaw;
    public final float pitch;

    public Look(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Look(Direction lookDirection) {
        this(DirectionUtils.getRequiredYaw(lookDirection), DirectionUtils.getRequiredPitch(lookDirection));
    }

    public Look(Direction lookDirectionYaw, Direction lookDirectionPitch) {
        this(DirectionUtils.getRequiredYaw(lookDirectionYaw), DirectionUtils.getRequiredPitch(lookDirectionPitch));
    }

    public Look(int rotation) {
        this(DirectionUtils.rotationToPlayerYaw(rotation), 0);
    }
}
