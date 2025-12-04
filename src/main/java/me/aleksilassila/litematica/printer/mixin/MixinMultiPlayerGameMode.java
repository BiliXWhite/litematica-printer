package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.interfaces.IMultiPlayerGameMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Override
    public void litematica_printer$rightClickBlock(BlockPos pos, Direction side, Vec3 hitVec)
    {
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
            //$$ClientLevel world,
            //#endif
            InteractionHand hand_1, BlockHitResult blockHitResult_1);

    @Shadow
    public abstract InteractionResult useItem(Player playerEntity_1,
                                              //#if MC < 11904
                                              //$$ Level world,
                                              //#endif
                                              InteractionHand hand_1);
}
