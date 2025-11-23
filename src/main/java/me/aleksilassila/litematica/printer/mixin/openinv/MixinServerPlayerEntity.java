package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.TickList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.playerlist;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.tickMap;

//#if MC == 11902
//$$ import org.jetbrains.annotations.Nullable;
//$$ import net.minecraft.network.encryption.PlayerPublicKey;
//#endif

// TODO(Ravel): can not resolve target class ServerPlayerEntity
// TODO(Ravel): can not resolve target class ServerPlayerEntity
@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity{

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
//    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile
//    //#if MC == 11902
//    //$$ , @Nullable PlayerPublicKey publicKey) { super(world, pos, yaw, profile, publicKey);
//    //#elseif MC > 12105
//    ) {super(world,  profile);
//    //#else
//    //$$ ) {super(world, pos, yaw, profile);
//    //#endif
//    }
//
//
    //#if MC < 11904
    //$$ @Inject(at = @At("HEAD"), method = "closeScreenHandler")
    //#else
    @Inject(at = @At("HEAD"), method = "onHandledScreenClosed")
    //#endif
    public void onHandledScreenClosed(CallbackInfo ci) {
        deletePlayerList();
    }
    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(at = @At("HEAD"), method = "onDisconnect")
    public void onDisconnect(CallbackInfo ci) {
        deletePlayerList();
    }

    @Unique
    private UUID getUuid1(){
        return ((ServerPlayerEntity)(Object)this).getUuid();
    }
    @Unique
    private void deletePlayerList(){
        playerlist.removeIf(player -> player.getUuid().equals(getUuid1()));
        List<Map.Entry<ServerPlayerEntity, TickList>> list = tickMap.entrySet().stream().filter(k -> k.getKey().getUuid().equals(getUuid1())).toList();
        for (Map.Entry<ServerPlayerEntity, TickList> serverPlayerEntityTickListEntry : list) {
            tickMap.remove(serverPlayerEntityTickListEntry.getKey());
        }
    }
    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"),method = "tick")
    public boolean onTick(ScreenHandler instance, PlayerEntity playerEntity, Operation<Boolean> original){
        if (playerEntity instanceof ServerPlayerEntity) {
            for (ServerPlayerEntity serverPlayerEntity : OpenInventoryPacket.playerlist) {
                if (serverPlayerEntity.equals(playerEntity)) return true;
            }
        }
        return instance.canUse(playerEntity);
    }
}
