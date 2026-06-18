package me.aleksilassila.litematica.printer.mixin.printer.mc;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinHud {
    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void init(CallbackInfo info) {}
}
