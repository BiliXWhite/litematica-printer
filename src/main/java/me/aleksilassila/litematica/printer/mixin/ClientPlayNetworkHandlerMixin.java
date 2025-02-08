package me.aleksilassila.litematica.printer.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
//    @Inject(method = "onEntityPosition",at = @At("HEAD"), cancellable = true)
//    public void onEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
//
//    }
//    @Inject(method = "onPlayerPositionLook",at = @At("HEAD"), cancellable = true)
//    public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
//
//    }
}
