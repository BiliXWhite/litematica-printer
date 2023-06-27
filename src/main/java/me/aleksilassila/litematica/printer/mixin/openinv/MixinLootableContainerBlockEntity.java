package me.aleksilassila.litematica.printer.mixin.openinv;


import me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {AbstractFurnaceBlockEntity.class,
        LootableContainerBlockEntity.class,
        BrewingStandBlockEntity.class
})
public class MixinLootableContainerBlockEntity extends BlockEntity {
    public MixinLootableContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Inject(at = @At("HEAD"),method = "canPlayerUse" , cancellable = true)
    public void canPlayerUse(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        for (ServerPlayerEntity player1 : OpenInventoryPacket.playerlist) {
//            System.out.println("1 "+player1.getEntityName());
//            System.out.println("2 "+player.getEntityName());
            if(player.equals(player1)) cir.setReturnValue(true);
        }
    }

    /**
     * @author 1
     * @reason 1
     */
//    @Overwrite
//    public boolean canPlayerUse(PlayerEntity player) {
//        for (ServerPlayerEntity player1 : OpenInventoryPacket.playerlist) {
//            if(player.equals(player1)) return true;
//        }
//        if (this.world.getBlockEntity(this.pos) != this) {
//            return false;
//        } else {
//            return !(player.squaredDistanceTo((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) > 64.0);
//        }
//    }
}
