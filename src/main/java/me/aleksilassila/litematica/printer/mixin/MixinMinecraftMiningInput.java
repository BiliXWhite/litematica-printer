package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftMiningInput {
    // Explicit stopDestroyBlock calls remain cancellations. Only the idle input loop is suppressed
    // while a mining or print-removal task still owns this exact target and client scope.
    @Inject(method = "continueAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V"), cancellable = true)
    private void preservePrinterMining(boolean attack, CallbackInfo ci) {
        if (!attack && Configs.Core.WORK_SWITCH.getBooleanValue()
                && (RuntimeAccess.get().modules().mine().shouldKeepMiningOnIdleInput()
                    || RuntimeAccess.get().modules().print().shouldKeepMiningOnIdleInput())) {
            ci.cancel();
        }
    }
}
