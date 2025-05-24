package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
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

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    //#if MC > 12004
    private void hookRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
    //#elseif MC > 11904
    //$$private void hookRenderHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
    //#else
    //$$private void hookRenderHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
    //#endif
        MinecraftClient client = MinecraftClient.getInstance();
        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();
        if (client.player != null) {
            // 检查玩家是否是观察者模式
            if (!client.player.isSpectator() && LitematicaMixinMod.PRINT_SWITCH.getBooleanValue() && LitematicaMixinMod.PRINTER_MODE.getOptionListValue().equals(State.PrintModeType.PRINTER)) {
                if (LitematicaMixinMod.RENDER_PROGRESS.getBooleanValue()) {
                    //#if MC > 11904
                    if (LitematicaMixinMod.LAG_CHECK.getBooleanValue())
                        context.drawCenteredTextWithShadow(client.textRenderer, Printer.packetTick + "Tick", (int) (width / 2), (int) (height / 2 - 22), new Color(255, 255, 255, 255).getRGB());
                    context.drawCenteredTextWithShadow(client.textRenderer, (int) (Printer.getPrinter().getPrintProgress() * 100) + "%", (int) (width / 2), (int) (height / 2 + 22), new Color(255, 255, 255, 255).getRGB());
                    context.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
                    context.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getPrinter().getPrintProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
                    //#else
                    //$$DrawableHelper.fill(matrices, (int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
                    //$$DrawableHelper.fill(matrices, (int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getPrinter().getPrintProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
                    //$$client.textRenderer.drawWithShadow(matrices, (int) (Printer.getPrinter().getPrintProgress() * 100) + "%", (int) ((width - client.textRenderer.getWidth((int) (Printer.getPrinter().getPrintProgress() * 100) + "%")) / 2), (int) (height / 2 + 22), new Color(255, 255, 255, 255).getRGB());
                    //$$if (LitematicaMixinMod.LAG_CHECK.getBooleanValue())
                    //$$    client.textRenderer.drawWithShadow(matrices, Printer.packetTick + "Tick", (int) ((width - client.textRenderer.getWidth(Printer.packetTick + "Tick")) / 2), (int) (height / 2 - 22), new Color(255, 255, 255, 255).getRGB());
                    //#endif
                }
            }
        }
    }
}
