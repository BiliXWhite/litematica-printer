package me.aleksilassila.litematica.printer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

//#if MC > 12001
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;

@Mixin(value = ClientCommonPacketListenerImpl.class)
//#else
//$$ import net.minecraft.client.multiplayer.ClientPacketListener;
//$$ @Mixin(value = ClientPacketListener.class)
//#endif
public class MixinClientCommonNetworkHandler {
    @Final
    @Shadow
    //#if MC > 11802
    protected Connection connection;
    //#else
    //$$ private Connection connection;
    //#endif

    @Final
    @Shadow
    //#if MC > 11802
    protected Minecraft minecraft;
    //#else
    //$$ private Minecraft minecraft;
    //#endif

    /**
     * @author zhaixianyu
     * @reason Fix look direction when printing while moving the camera
     */
    //#if MC < 12004
    //$$ @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"),method = "send(Lnet/minecraft/network/protocol/Packet;)V")
    //#else
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"), method = "send")
    //#endif
    public void sendPacket(Connection instance, Packet<?> packet, Operation<Void> original) {
        //TODO: 待实现，临时注释掉
//        if (packet instanceof ServerboundMovePlayerPacketAccessor accessor) {
//            accessor.setPitch(PlayerLookUtils.getYaw(accessor.getYaw()));
//            accessor.setPitch(PlayerLookUtils.getPitch(accessor.getPitch()));
//            accessor.setOnGround(PlayerLookUtils.getOnGround(accessor.getOnGround()));
//            accessor.setHasRot(PlayerLookUtils.getHasRot(accessor.getHasRot()));
//        }
        Direction directionYaw = Printer.getInstance().queue.lookDirYaw;
        Direction directionPitch = Printer.getInstance().queue.lookDirPitch;
        if ((directionYaw != null || directionPitch != null) && Implementation.isLookAndMovePacket(packet)) {
            Packet<?> fixedPacket = Implementation.getFixedLookPacket(minecraft.player, packet, directionYaw, directionPitch);
            if (fixedPacket != null) {
                this.connection.send(fixedPacket);
                return;
            }
        } else if (directionYaw == null || directionPitch == null || !Implementation.isLookOnlyPacket(packet)) {
            this.connection.send(packet);
            return;
        }
        original.call(instance, packet);
    }
}
