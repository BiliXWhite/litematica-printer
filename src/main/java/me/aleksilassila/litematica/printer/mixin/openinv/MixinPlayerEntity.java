package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Player.class)
public class MixinPlayerEntity {

    //FIXME 等待宅咸鱼更新远程交互
    //#if MC < 12109
    //$$ @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;canUse(Lnet/minecraft/entity/player/PlayerEntity;)Z"),method = "tick")
    //$$ public boolean tick(ScreenHandler instance, PlayerEntity playerEntity, Operation<Boolean> original){
    //$$     if (playerEntity instanceof ServerPlayerEntity) {
    //$$         for (ServerPlayerEntity serverPlayerEntity : OpenInventoryPacket.playerlist) {
    //$$             if (serverPlayerEntity.equals(playerEntity)) return true;
    //$$         }
    //$$     }
    //$$     return instance.canUse(playerEntity);
    //$$ }
    //#endif
}
