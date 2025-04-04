package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//$$import net.minecraft.client.util.math.MatrixStack;
//$$import org.lwjgl.opengl.GL11;
//$$import org.lwjgl.opengl.GL11C;
//#endif

import java.awt.*;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    //#if MC > 11904
    private void hookRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
    //#else
    //$$private void hookRenderHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
    //#endif
        MinecraftClient client = MinecraftClient.getInstance();
        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();
        if (client.player != null) {
            // 检查玩家是否是观察者模式
            if (!client.player.isSpectator() && LitematicaMixinMod.RENDER_PROGRESS.getBooleanValue() && LitematicaMixinMod.PRINT_SWITCH.getBooleanValue()) {
                //#if MC > 11904
                context.drawCenteredTextWithShadow(client.textRenderer,  (int) (Printer.getPrinter().getPrintProgress() * 100) + "%", (int) (width / 2), (int) (height / 2 + 22), new Color(255, 255, 255, 255).getRGB());
                context.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
                context.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getPrinter().getPrintProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
                //#else
                //$$client.textRenderer.drawWithShadow(matrices, (int) (Printer.getPrinter().getPrintProgress() * 100) + "%", (int) (width / 2), (int) (height / 2 + 10), new Color(255, 255, 255, 255).getRGB());
                //$$drawRect((width / 2 - 20), (height / 2 + 24), (width / 2 + 20), (height / 2 + 36), new Color(0, 0, 0, 150).getRGB());
                //$$drawRect((width / 2 - 20), (height / 2 + 24), (width / 2 + 20), (height / 2 + 24 + Printer.getPrinter().getPrintProgress() * 12), new Color(0, 255, 0, 255).getRGB());
                //#endif
            }
        }
    }


//#if MC <= 11904
//$$    private static void drawRect(double x1, double y1, double x2, double y2, int color) {
//$$        float a = (color >> 24 & 0xFF) / 255.0F;
//$$        float r = (color >> 16 & 0xFF) / 255.0F;
//$$        float g = (color >> 8 & 0xFF) / 255.0F;
//$$        float b = (color & 0xFF) / 255.0F;
//$$        RenderSystem.enableBlend();
//$$        GL11C.glDisable(GL11.GL_TEXTURE_2D);
//$$        RenderSystem.defaultBlendFunc();
//$$        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
//$$        Tessellator tess = Tessellator.getInstance();
//$$        BufferBuilder buffer = tess.getBuffer();
//$$        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//$$        buffer.vertex(x1, y2, 0).color(r, g, b, a);
//$$        buffer.vertex(x2, y2, 0).color(r, g, b, a);
//$$        buffer.vertex(x2, y1, 0).color(r, g, b, a);
//$$        buffer.vertex(x1, y1, 0).color(r, g, b, a);
//$$        tess.getBuffer().end();
//$$        GL11C.glEnable(GL11.GL_TEXTURE_2D);
//$$        RenderSystem.disableBlend();
//$$    }
//#endif
}
