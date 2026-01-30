package me.aleksilassila.litematica.printer.mixin.printer.mc;

import com.mojang.logging.LogUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interfaces.IMultiPlayerGameMode;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.NetworkUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC < 11904
//$$ import net.minecraft.world.level.Level;
//$$ import net.minecraft.client.multiplayer.ClientLevel;
//$$
//#endif

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IMultiPlayerGameMode {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Unique
    private ServerboundPlayerActionPacket getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action action, BlockPos blockPos, Direction direction, int sequence) {
        //#if MC > 11802
        return new ServerboundPlayerActionPacket(action, blockPos, direction, sequence);
        //#else
        //$$ return new ServerboundPlayerActionPacket(action, blockPos, direction);
        //#endif
    }

    @Override
    public void litematica_printer$rightClickBlock(BlockPos pos, Direction side, Vec3 hitVec) {
        useItemOn(minecraft.player,
                //#if MC < 11904
                //$$ minecraft.level,
                //#endif
                InteractionHand.MAIN_HAND,
                new BlockHitResult(hitVec, side, pos, false));
        useItem(minecraft.player,
                //#if MC < 11904
                //$$ minecraft.level,
                //#endif
                InteractionHand.MAIN_HAND);
    }

    @Shadow
    public abstract InteractionResult useItemOn(
            LocalPlayer clientPlayerEntity_1,
            //#if MC < 11904
            //$$ClientLevel level,
            //#endif
            InteractionHand hand_1, BlockHitResult blockHitResult_1);

    @Shadow
    public abstract InteractionResult useItem(Player playerEntity_1,
                                              //#if MC < 11904
                                              //$$ Level level,
                                              //#endif
                                              InteractionHand hand_1);

    @Inject(at = @At("HEAD"), method = "startDestroyBlock", cancellable = true)
    public void startDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionUtils.INSTANCE.isDestroying()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "continueDestroyBlock", cancellable = true)
    public void continueDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionUtils.INSTANCE.isDestroying()) {
            cir.setReturnValue(false);
        }
    }
}
