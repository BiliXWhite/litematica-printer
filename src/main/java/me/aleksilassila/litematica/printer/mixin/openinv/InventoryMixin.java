package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO(Ravel): can not resolve target class Inventory
// TODO(Ravel): can not resolve target class Inventory
@Mixin(Inventory.class)
public interface InventoryMixin {
    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(at = @At("HEAD"), method = "canPlayerUse(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/player/PlayerEntity;F)Z", cancellable = true)
    private static void canPlayeruse(BlockEntity blockEntity, PlayerEntity player, float range, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity) {
            for (ServerPlayerEntity serverPlayerEntity : OpenInventoryPacket.playerlist) {
                if (serverPlayerEntity.equals(player)) cir.setReturnValue(true);
            }
        }
    }
}
