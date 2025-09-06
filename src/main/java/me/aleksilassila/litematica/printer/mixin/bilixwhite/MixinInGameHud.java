package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
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
            if (!client.player.isSpectator() && LitematicaMixinMod.PRINT_SWITCH.getBooleanValue() &&
                    (LitematicaMixinMod.PRINTER_MODE.getOptionListValue().equals(State.PrintModeType.PRINTER)
                    ||LitematicaMixinMod.PRINTER_MODE.getOptionListValue().equals(State.PrintModeType.MINE))) {
                if (LitematicaMixinMod.RENDER_HUD.getBooleanValue()) {
                    //#if MC <= 11904
                    //$$ StringUtils.initMatrix(matrices);
                    //#else
                    StringUtils.initDrawContext(context);
                    //#endif
                    if (LitematicaMixinMod.LAG_CHECK.getBooleanValue())
                        StringUtils.drawText(Printer.packetTick + "Tick", (int) (width / 2), (int) (height / 2 - 22), new Color(255, 255, 255, 255).getRGB(), true, true);

                    StringUtils.drawText(LitematicaMixinMod.PRINTER_MODE.getOptionListValue().getDisplayName(), (int) (width / 2), (int) (height / 2 + 42), new Color(255, 255, 255, 255).getRGB(), true, true);

                    if (Printer.requiredState != null)
                        StringUtils.drawText(Printer.requiredState.getBlock().getName().getString(), (int) (width / 2), (int) (height / 2 + 54), new Color(255, 255, 255, 255).getRGB(), true, true);
                }
            }
        }
    }
}
