package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.TickList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.playerlist;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.tickMap;


@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity{

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
    //#if MC < 11904
    //$$ @Inject(at = @At("HEAD"), method = "closeScreenHandler")
    //#else
    @Inject(at = @At("HEAD"), method = "onHandledScreenClosed")
    //#endif
    public void onHandledScreenClosed(CallbackInfo ci) {
        deletePlayerList();
    }
    @Inject(at = @At("HEAD"), method = "onDisconnect")
    public void onDisconnect(CallbackInfo ci) {
        deletePlayerList();
    }
    @Unique
    private void deletePlayerList(){
        playerlist.removeIf(player -> player.getUuid().equals(getUuid()));
        List<Map.Entry<ServerPlayerEntity, TickList>> list = tickMap.entrySet().stream().filter(k -> k.getKey().getUuid().equals(getUuid())).toList();
        for (Map.Entry<ServerPlayerEntity, TickList> serverPlayerEntityTickListEntry : list) {
            tickMap.remove(serverPlayerEntityTickListEntry.getKey());
        }
    }
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;canUse(Lnet/minecraft/entity/player/PlayerEntity;)Z"),method = "tick")
    public boolean onTick(ScreenHandler instance, PlayerEntity playerEntity, Operation<Boolean> original){
        if (playerEntity instanceof ServerPlayerEntity) {
            for (ServerPlayerEntity serverPlayerEntity : OpenInventoryPacket.playerlist) {
                if (serverPlayerEntity.equals(playerEntity)) return true;
            }
        }
        return instance.canUse(playerEntity);
    }
}
