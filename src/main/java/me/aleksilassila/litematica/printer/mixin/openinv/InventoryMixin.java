package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public interface InventoryMixin {

    @Inject(
            at = @At("HEAD"),
            //#if MC >= 12004
            method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;F)Z",
            //#else
            //$$ method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;I)Z",
            //#endif
            cancellable = true
    )
    private static void canPlayeruse(BlockEntity blockEntity,
                                     Player player,
                                     //#if MC >= 12004
                                     float range,
                                     //#else
                                     //$$ int range,
                                     //#endif
                                     CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer) {
            for (ServerPlayer serverPlayer : OpenInventoryPacket.playerList) {
                if (serverPlayer.equals(player)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }


}
