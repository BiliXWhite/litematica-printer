package me.aleksilassila.litematica.printer.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Accessor("x")
    double getX();

    @Accessor("y")
    double getY();

    @Accessor("z")
    double getZ();

    @Accessor("yRot")
    float getYaw();

    @Accessor("xRot")
    float getPitch();

    @Accessor("onGround")
    boolean getOnGround();

    @Accessor("hasRot")
    boolean getHasRot();

    @Accessor("x")
    void setX(double x);

    @Accessor("y")
    void setY(double y);

    @Accessor("z")
    void setZ(double z);

    @Accessor("yRot")
    void setYaw(float yaw);

    @Accessor("xRot")
    void setPitch(float pitch);

    @Accessor("onGround")
    void setOnGround(boolean onGround);

    @Accessor("hasRot")
    void setHasRot(boolean hasRot);
}
