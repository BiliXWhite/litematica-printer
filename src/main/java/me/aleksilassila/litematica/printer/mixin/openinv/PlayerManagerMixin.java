package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
//#if MC > 12001
import net.minecraft.server.network.ConnectedClientData;
//#endif
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO(Ravel): can not resolve target class PlayerManager
// TODO(Ravel): can not resolve target class PlayerManager
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(at = @At("TAIL"), method = "onPlayerConnect")
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player,
                                 //#if MC > 12001
                                 ConnectedClientData clientData,
                                 //#endif
                                 CallbackInfo ci) {
        OpenInventoryPacket.helloRemote(player);
    }
}
