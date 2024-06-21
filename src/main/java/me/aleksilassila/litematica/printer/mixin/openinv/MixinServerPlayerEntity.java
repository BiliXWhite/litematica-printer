package me.aleksilassila.litematica.printer.mixin.openinv;

import com.mojang.authlib.GameProfile;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket.playerlist;


@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity{

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
    //#if MC < 11904
    //$$ @Inject(at = @At("HEAD"), method = "closeHandledScreen")
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
    }
}
