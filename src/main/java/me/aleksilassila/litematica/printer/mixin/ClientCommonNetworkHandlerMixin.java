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
public class ClientCommonNetworkHandlerMixin {

    @Final
    @Shadow
    protected Connection connection;

    @Final
    @Shadow
    protected Minecraft minecraft;

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
        Direction directionYaw = Printer.getPrinter().queue.lookDirYaw;
        Direction directionPitch = Printer.getPrinter().queue.lookDirPitch;
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
