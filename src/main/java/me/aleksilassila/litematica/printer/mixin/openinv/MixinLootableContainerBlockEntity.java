package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {AbstractFurnaceBlockEntity.class,
        RandomizableContainerBlockEntity.class,
        BrewingStandBlockEntity.class
})
public class MixinLootableContainerBlockEntity extends BlockEntity {
    public MixinLootableContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    //#if MC > 12004
    //#else
    //$$ @Inject(at = @At("HEAD"), method = "stillValid", cancellable = true)
    //$$  public void canPlayerUse(Player player, CallbackInfoReturnable<Boolean> cir) {
    //$$      for (ServerPlayer player1 : OpenInventoryPacket.playerList) {
    //$$          if (player.equals(player1)) {
    //$$              cir.setReturnValue(true);
    //$$          }
    //$$      }
    //$$  }
    //#endif
}