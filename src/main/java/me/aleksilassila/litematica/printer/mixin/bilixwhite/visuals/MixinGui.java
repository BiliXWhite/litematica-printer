package me.aleksilassila.litematica.printer.mixin.bilixwhite.visuals;

import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//$$import com.mojang.blaze3d.vertex.PoseStack;
//$$import org.lwjgl.opengl.GL11;
//$$import org.lwjgl.opengl.GL11C;
//#elseif MC > 12006
import net.minecraft.client.DeltaTracker;
//#endif

import java.awt.*;

import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.MODE_SWITCH;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Inject(method = "renderItemHotbar", at = @At("TAIL"))
    //#if MC > 12006
    private void hookRenderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    //#elseif MC > 11904
    //$$private void hookRenderItemHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
    //#else
    //$$private void hookRenderItemHotbar(float f, PoseStack poseStack, CallbackInfo ci) {
    //#endif
        Minecraft mc = Minecraft.getInstance();
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        if (mc.player != null) {
            // 检查玩家是否是观察者模式
            if (!mc.player.isSpectator() && LitematicaPrinterMod.PRINT_SWITCH.getBooleanValue() && !PrinterUtils.isBedrockMode()) {
                if (LitematicaPrinterMod.RENDER_HUD.getBooleanValue()) {
                    //#if MC <= 11904
                    //$$ StringUtils.initMatrix(poseStack);
                    //#else
                    StringUtils.initGuiGraphics(guiGraphics);
                    //#endif

//                    if (Printer.requiredState != null) {
//                        StringUtils.drawText("投影：" + Printer.requiredState.getBlock().getName().getString(), 50, 50, Color.CYAN.getRGB(), true);
//                        StringUtils.drawText("实际: " + Printer.currentState.getBlock().getName().getString(), 50, 59, Color.ORANGE.getRGB(), true);
//                        StringUtils.drawText("投影液体：" + Printer.requiredState.getFluidState().getBlockState().getBlock().getName().getString(), 50, 68, Color.CYAN.getRGB(), true);
//                        StringUtils.drawText("实际液体: " + Printer.currentState.getFluidState().getBlockState().getBlock().getName().getString() + " " + Printer.currentState.getFluidState().getBlockState().getBlock().toString(), 50, 77, Color.ORANGE.getRGB(), true);
//                    }

                    if (LitematicaPrinterMod.LAG_CHECK.getBooleanValue()) {
                        StringUtils.drawString(Printer.packetTick + "Tick", (int) (width / 2), (int) (height / 2 - 22), new Color(255, 255, 255, 255).getRGB(), true, true);
                    }
                    if (MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE) ) {
                        StringUtils.drawString((int) (Printer.getPrinter().getProgress() * 100) + "%", (int) (width / 2), (int) (height / 2 + 22), new Color(255, 255, 255, 255).getRGB(), true, true);
                        //#if MC > 11904
                        guiGraphics.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
                        guiGraphics.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getPrinter().getProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
                        //#else
                        //$$ GuiComponent.fill(poseStack, (int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
                        //$$ GuiComponent.fill(poseStack, (int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getPrinter().getProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
                        //#endif
                    }
                    StringUtils.drawString(LitematicaPrinterMod.PRINTER_MODE.getOptionListValue().getDisplayName(), (int) (width / 2), (int) (height / 2 + 52), new Color(255, 255, 255, 255).getRGB(), true, true);

                    if (Printer.requiredState != null)
                        StringUtils.drawString(Printer.requiredState.getBlock().getName().getString(), (int) (width / 2), (int) (height / 2 + 64), new Color(255, 255, 255, 255).getRGB(), true, true);
                }
            }
        }
    }
}
