package me.aleksilassila.litematica.printer.mixin.openinv;

//#if MC > 11802 && MC < 12005
//$$ import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
//$$ import net.minecraft.block.entity.BlockEntity;
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import net.minecraft.inventory.Inventory;
//$$ import net.minecraft.server.network.ServerPlayerEntity;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Mixin(Inventory.class)
//$$ public interface InventoryMixin {
//$$     @Inject(at = @At("HEAD"), method = "canPlayerUse(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/player/PlayerEntity;I)Z", cancellable = true)
//$$     private static void canPlayeruse(BlockEntity blockEntity, PlayerEntity player, int range, CallbackInfoReturnable<Boolean> cir) {
//$$         if (player instanceof ServerPlayerEntity) {
//$$             for (ServerPlayerEntity serverPlayerEntity : OpenInventoryPacket.playerlist) {
//$$                 if (serverPlayerEntity.equals(player)) cir.setReturnValue(true);
//$$             }
//$$         }
//$$     }
//$$ }
//#endif