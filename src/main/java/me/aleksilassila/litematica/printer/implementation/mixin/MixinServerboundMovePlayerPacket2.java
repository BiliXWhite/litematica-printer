package me.aleksilassila.litematica.printer.implementation.mixin;

import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.Printer2;
import me.aleksilassila.litematica.printer.actions.PrepareAction;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.utils.PlayerLookUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ServerboundMovePlayerPacket.class, priority = 1010)
public class MixinServerboundMovePlayerPacket2 {
    //#if MC > 12101
    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    //#else
    //$$ @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    //#endif
    private static float modifyLookYaw(float yaw) {
        Printer2 printer = LitematicaPrinterMod.printer;
        if (printer == null) {
            return yaw;
        }
        PrepareAction action = printer.actionHandler.lookAction;
        if (action != null && action.modifyYaw) {
            Debug.write("YAW: {}", action.yaw);
            return action.yaw;
        } else {
            return yaw;
        }
    }


    //#if MC > 12101
    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    //#else
    //$$ @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    //#endif
    private static float modifyLookPitch(float pitch) {
        Printer2 printer = LitematicaPrinterMod.printer;
        if (printer == null) {
            return pitch;
        }
        PrepareAction action = printer.actionHandler.lookAction;
        if (action != null && action.modifyPitch) {
            Debug.write("PITCH: {}", action.pitch);
            return action.pitch;
        } else {
            return pitch;
        }
    }
}
