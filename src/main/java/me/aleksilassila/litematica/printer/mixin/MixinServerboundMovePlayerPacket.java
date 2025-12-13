package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.utils.PlayerLookUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ServerboundMovePlayerPacket.class, priority = 1010)
public class MixinServerboundMovePlayerPacket {
    //#if MC > 12101
    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    //#else
    //$$ @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    //#endif
    private static float modifyLookYaw(float yaw) {
        return PlayerLookUtils.getYaw(yaw);
    }


    //#if MC > 12101
    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    //#else
    //$$ @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    //#endif
    private static float modifyLookPitch(float pitch) {
        return PlayerLookUtils.getPitch(pitch);
    }

    //#if MC > 12101
    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    //#else
    //$$ @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    //#endif
    private static boolean modifyHasRot(boolean hasRot) {
        return PlayerLookUtils.isModifying();
    }
}
