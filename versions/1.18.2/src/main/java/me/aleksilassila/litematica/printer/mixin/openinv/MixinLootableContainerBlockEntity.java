package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
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

    @Inject(at = @At("HEAD"), method = "stillValid", cancellable = true)
    public void canPlayerUse(Player player, CallbackInfoReturnable<Boolean> cir) {
        for (ServerPlayer player1 : OpenInventoryPacket.playerlist) {
            if (player.equals(player1)) cir.setReturnValue(true);
        }
    }
}
