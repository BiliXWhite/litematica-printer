package me.aleksilassila.litematica.printer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.Printer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.cancelMovePack;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 12001
import net.minecraft.client.network.ClientCommonNetworkHandler;

@Mixin(value = ClientCommonNetworkHandler.class)
//#else
//$$ import net.minecraft.client.network.ClientPlayNetworkHandler;
//$$ @Mixin(ClientPlayNetworkHandler.class)
//#endif
public class ClientCommonNetworkHandlerMixin {
    @Final
    @Shadow
    protected ClientConnection connection;

    @Final
    @Shadow
    protected MinecraftClient client;

    /**
     * @author 6
     * @reason 6
     */

    //#if MC < 12004
    //$$ @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V"),method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
    //#else
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V"), method = "sendPacket", cancellable = true)
    //#endif
    public void sendPacket(Packet<?> packet, CallbackInfo ci) {
        if (Printer.currentAction == null) {
            return;
        }

        Direction direction = Printer.currentAction.lookDirection;
        if (direction != null) {
            if (packet instanceof PlayerMoveC2SPacket.Full full) {
                Packet<?> fixedPacket = Implementation.getFixedLookPacket(client.player, full, Printer.currentAction);
                if (fixedPacket != null) {
                    this.connection.send(fixedPacket);
                    ci.cancel();
                }
            } else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                ci.cancel();
            }
        }
    }
}
