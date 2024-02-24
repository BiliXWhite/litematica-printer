package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GenericContainerScreenHandler.class)
public class MixinGenericContainerScreenHandler {
    @Inject(at = @At("HEAD"), method = "onClosed",cancellable = true,locals = LocalCapture.CAPTURE_FAILHARD)
    public void onClosed(PlayerEntity player, CallbackInfo ci) {
        if(!(player instanceof ServerPlayerEntity)) return;
        for (ServerPlayerEntity player1 : OpenInventoryPacket.playerlist) {
            if (player.equals(player1)) {
                ci.cancel();
            }
        }
    }
}
