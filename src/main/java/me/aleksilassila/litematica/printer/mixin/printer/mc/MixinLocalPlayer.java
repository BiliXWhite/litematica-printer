package me.aleksilassila.litematica.printer.mixin.printer.mc;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.BlockCooldownManager;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.UpdateCheckerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

//#if MC >= 12001 && MC <= 12104
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
//$$ import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
//#else
//#endif
//#if MC >= 12105
import java.net.URI;
//#endif

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer extends AbstractClientPlayer {
    @Final
    @Shadow
    public ClientPacketListener connection;

    @Final
    @Shadow
    protected Minecraft minecraft;

    //#if MC == 11902
    //$$ public MixinLocalPlayer(ClientLevel world, GameProfile profile, @Nullable PlayerPublicKey publicKey) {
    //$$    super(world, profile, publicKey);
    //$$ }
    //#else
    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }
    //#endif

    @Inject(at = @At("HEAD"), method = "resetPos")
    public void init(CallbackInfo ci) {
        if (Configs.General.UPDATE_CHECK.getBooleanValue() && !Printer.getInstance().updateChecked) {
            CompletableFuture.runAsync(UpdateCheckerUtils::checkForUpdates);
        }
        Printer.getInstance().updateChecked = true;
    }

    @Inject(at = @At("HEAD"), method = "closeContainer")
    public void close(CallbackInfo ci) {
        //#if MC >= 12001 && MC <= 12104
        //$$ if(ModLoadStatus.isLoadChestTrackerLoaded()) MemoryUtils.saveMemory(this.containerMenu);
        //$$ OpenInventoryPacket.reSet();
        //#endif
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {
        BlockCooldownManager.INSTANCE.tick();
        ZxyUtils.tick();
        InteractionUtils.INSTANCE.onTick();
        Printer printer = Printer.getInstance();
        printer.onGameTick(minecraft, minecraft.level, (LocalPlayer) (Object) this);
    }


    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    //#if MC > 11904
    public void openTextEdit(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        openEditSignScreen(sign, front, ci);
    }
    //#else
    //$$ public void openTextEdit(SignBlockEntity sign, CallbackInfo ci) {
    //$$    openEditSignScreen(sign, false, ci);
    //$$ }
    //#endif

    public void openEditSignScreen(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        getTargetSignEntity(sign).ifPresent(signBlockEntity ->
        {
            //#if MC > 11904
            String line1 = signBlockEntity.getText(front).getMessage(0, false).getString();
            String line2 = signBlockEntity.getText(front).getMessage(1, false).getString();
            String line3 = signBlockEntity.getText(front).getMessage(2, false).getString();
            String line4 = signBlockEntity.getText(front).getMessage(3, false).getString();
            //#else
            //$$ String line1 = signBlockEntity.getMessage(0, false).getString();
            //$$ String line2 = signBlockEntity.getMessage(1, false).getString();
            //$$ String line3 = signBlockEntity.getMessage(2, false).getString();
            //$$ String line4 = signBlockEntity.getMessage(3, false).getString();
            //#endif
            ServerboundSignUpdatePacket packet = new ServerboundSignUpdatePacket(sign.getBlockPos(),
                    //#if MC > 11904
                    front,
                    //#endif
                    line1,
                    line2,
                    line3,
                    line4
            );
            this.connection.send(packet);
            ci.cancel();
        });
    }

    @Unique
    private Optional<SignBlockEntity> getTargetSignEntity(SignBlockEntity sign) {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (sign.getLevel() == null || worldSchematic == null) {
            return Optional.empty();
        }
        BlockEntity targetBlockEntity = worldSchematic.getBlockEntity(sign.getBlockPos());
        if (targetBlockEntity instanceof SignBlockEntity targetSignEntity) {
            return Optional.of(targetSignEntity);
        }
        return Optional.empty();
    }
}