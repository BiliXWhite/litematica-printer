package me.aleksilassila.litematica.printer.mixin.masa;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("TAIL"), method = "renderLayer")
    private void test(RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, CallbackInfo ci) {
//        if(client.player!=null) HighlightBlockRenderer.test2(client.player.getBlockPos().up(-1),matrices,positionMatrix);
//        if(client.player!=null) HighlightBlockRenderer.test3(matrices);
//        if(client.player!=null) HighlightBlockRenderer.test(client.player.getBlockPos().up(-1));
    }
}
