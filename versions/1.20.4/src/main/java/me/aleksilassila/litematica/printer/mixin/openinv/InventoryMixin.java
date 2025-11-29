package me.aleksilassila.litematica.printer.mixin.openinv;


import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public interface InventoryMixin {
    @Inject(at = @At("HEAD"), method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;I)Z", cancellable = true)
    private static void canPlayeruse(BlockEntity blockEntity, Player player, int range, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer) {
            for (ServerPlayer serverPlayer : OpenInventoryPacket.playerlist) {
                if (serverPlayer.equals(player)) cir.setReturnValue(true);
            }
        }
    }
}