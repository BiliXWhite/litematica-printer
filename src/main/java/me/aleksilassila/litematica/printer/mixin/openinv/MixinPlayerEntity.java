package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;


//#if MC > 12108
@Mixin(value = ServerPlayer.class)
//#else
//$$ @Mixin(value = Player.class)
//#endif
public class MixinPlayerEntity {
    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"
            ),
            method = "tick"
    )
    public boolean tick$stillValid(AbstractContainerMenu instance, Player player, Operation<Boolean> original) {
        if (player instanceof ServerPlayer) {
            for (ServerPlayer serverPlayer : OpenInventoryPacket.playerList) {
                if (serverPlayer.equals(player)) return true;
            }
        }
        return instance.stillValid(player);
    }
}
