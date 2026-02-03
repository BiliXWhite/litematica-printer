package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.TickList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.playerList;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.tickMap;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity {
    @Inject(at = @At("HEAD"), method = "disconnect")
    public void onDisconnect(CallbackInfo ci) {
        deletePlayerList();
    }

    @Inject(at = @At("HEAD"), method = "doCloseContainer")
    public void onHandledScreenClosed(CallbackInfo ci) {
        deletePlayerList();
    }

    @Unique
    private UUID getUuid1() {
        return ((ServerPlayer) (Object) this).getUUID();
    }

    @Unique
    private void deletePlayerList() {
        playerList.removeIf(player -> player.getUUID().equals(getUuid1()));
        List<Map.Entry<ServerPlayer, TickList>> list = tickMap
                .entrySet()
                .stream()
                .filter(k -> k.getKey().getUUID().equals(getUuid1()))
                .toList();
        for (Map.Entry<ServerPlayer, TickList> serverPlayerEntityTickListEntry : list) {
            tickMap.remove(serverPlayerEntityTickListEntry.getKey());
        }
    }

    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"
            ),
            method = "tick"
    )
    public boolean onTick(AbstractContainerMenu instance, Player playerEntity, Operation<Boolean> original) {
        if (playerEntity instanceof ServerPlayer) {
            for (ServerPlayer serverPlayerEntity : OpenInventoryPacket.playerList) {
                if (serverPlayerEntity.equals(playerEntity)) {
                    return true;
                }
            }
        }
        return instance.stillValid(playerEntity);
    }
}
