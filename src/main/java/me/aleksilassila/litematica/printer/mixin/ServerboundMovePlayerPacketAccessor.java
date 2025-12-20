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

    @Accessor("onGround")
    boolean getOnGround();

    //#if MC > 12101
    @Accessor("horizontalCollision")
    boolean getHorizontalCollision();
    //#endif
}
