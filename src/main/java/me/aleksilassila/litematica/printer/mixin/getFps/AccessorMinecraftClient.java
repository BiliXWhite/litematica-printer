package me.aleksilassila.litematica.printer.mixin.getFps;


import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface AccessorMinecraftClient {
    @Accessor("currentFps")
    int getCurrentFps();
    @Accessor("fpsCounter")
    int getFpsCounter();
}
