package me.aleksilassila.litematica.printer.mixin.openinv;

//#if MC < 12109
//$$import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
//$$import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
//$$import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
//$$import org.spongepowered.asm.mixin.injection.At;
//#endif

@Mixin(value = Player.class)
public class MixinPlayerEntity {
    //FIXME 等待宅咸鱼更新远程交互
    //#if MC < 12109

    //$$ @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"), method = "tick")
    //$$ public boolean tick(AbstractContainerMenu instance, Player player, Operation<Boolean> original) {
    //$$     if (player instanceof ServerPlayer) {
    //$$         for (ServerPlayer serverPlayer : OpenInventoryPacket.playerlist) {
    //$$             if (serverPlayer.equals(player)) return true;
    //$$         }
    //$$     }
    //$$     return instance.stillValid(player);
    //$$ }
    //#endif
}
