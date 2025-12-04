package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestMenu.class)
public class MixinGenericContainerScreenHandler {
    @Inject(at = @At("HEAD"), method = "removed", cancellable = true)
    public void onClosed(Player player, CallbackInfo ci) {
        if(!(player instanceof ServerPlayer)) return;
        for (ServerPlayer player1 : OpenInventoryPacket.playerlist) {
            if (player.equals(player1)) {
                ci.cancel();
            }
        }
    }
}
