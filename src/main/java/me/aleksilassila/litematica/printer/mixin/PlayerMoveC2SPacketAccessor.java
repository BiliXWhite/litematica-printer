package me.aleksilassila.litematica.printer.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// TODO(Ravel): can not resolve target class PlayerMoveC2SPacket
// TODO(Ravel): can not resolve target class PlayerMoveC2SPacket
@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Accessor("x")
    double getX();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Accessor("y")
    double getY();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Accessor("z")
    double getZ();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Accessor("yaw")
    float getYaw();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Accessor("onGround")
    boolean getOnGround();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Accessor("changePosition")
    boolean changePosition();
}
