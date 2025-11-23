package me.aleksilassila.litematica.printer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.Printer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

//#if MC > 12001
import net.minecraft.client.network.ClientCommonNetworkHandler;
// TODO(Ravel): can not resolve target class ClientCommonNetworkHandler
// TODO(Ravel): can not resolve target class ClientCommonNetworkHandler
@Mixin(value = ClientCommonNetworkHandler.class)
//#else
//$$ import net.minecraft.client.network.ClientPlayNetworkHandler;
//$$ @Mixin(ClientPlayNetworkHandler.class)
//#endif
public class ClientCommonNetworkHandlerMixin {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Final
    @Shadow
    protected ClientConnection connection;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Final
    @Shadow
    protected MinecraftClient client;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
/**
     * @author 6
     * @reason 6
     */
//    @Overwrite
    //#if MC < 12004
    //$$ @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V"),method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    //#else
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"),method = "sendPacket")
    //#endif
    public void sendPacket(ClientConnection instance, Packet<?> packet, Operation<Void> original) {
        Direction directionYaw = Printer.getPrinter().queue.lookDirYaw;
        Direction directionPitch = Printer.getPrinter().queue.lookDirPitch;
        if ((directionYaw != null || directionPitch != null) && Implementation.isLookAndMovePacket(packet)) {
            Packet<?> fixedPacket = Implementation.getFixedLookPacket(client.player, packet, directionYaw, directionPitch);
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
