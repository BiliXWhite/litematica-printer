package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC > 12001
import net.minecraft.server.network.CommonListenerCookie;
//#endif

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(at = @At("TAIL"), method = "placeNewPlayer")
    private void placeNewPlayer(Connection connection, ServerPlayer player,
                                //#if MC > 12001
                                CommonListenerCookie clientData,
                                //#endif
                                CallbackInfo ci) {
        OpenInventoryPacket.helloRemote(player);
    }
}
