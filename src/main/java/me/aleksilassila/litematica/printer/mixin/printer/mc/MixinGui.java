package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.bilixwhite.utils.RenderUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.GuiBlockInfo;
import me.aleksilassila.litematica.printer.handler.Handlers;
import me.aleksilassila.litematica.printer.printer.BlockContext;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

@Mixin(Gui.class)
public abstract class MixinGui {
    @Unique
    private int lastCount;

    @Inject(method = "renderItemHotbar", at = @At("TAIL"))
    //#if MC > 12006
    private void hookRenderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        //#elseif MC > 12004
        //$$ private void hookRenderItemHotbar(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        //#elseif MC > 11904
        //$$ private void hookRenderItemHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        //#else
        //$$ private void hookRenderItemHotbar(float f, PoseStack poseStack, CallbackInfo ci) {
        //#endif
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();

        if (player.isSpectator()) return;
        if (!Printer.isEnable()) return;
        if (PrinterUtils.isBedrockMode()) return;
        if (!Configs.Core.RENDER_HUD.getBooleanValue()) return;

        //#if MC <= 11904
        //$$ RenderUtils.initMatrix(poseStack);
        //#else
        RenderUtils.initGuiGraphics(guiGraphics);
        //#endif

        BlockContext context = Printer.getInstance().blockContext;
        if (Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
            int x = 50;
            int y = 50;
            if (PrinterUtils.isPrinterMode() && context != null) {
                RenderUtils.drawString("位置: " + context.blockPos.toShortString(), x, y, Color.CYAN.getRGB(), true);
                y += 9;
                RenderUtils.drawString("投影: " + context.getRequiredBlockNameString(), x, y, Color.CYAN.getRGB(), true);
                y += 9;
                RenderUtils.drawString("实际: " + context.getCurrentBlockNameString(), x, y, Color.ORANGE.getRGB(), true);
            } else {
                for (ClientPlayerTickHandler handler : Handlers.VALUES) {
                    GuiBlockInfo gui = handler.getGuiBlockInfo();
                    if (gui == null) continue;
                    Block block = gui.state.getBlock();
                    BlockPos blockPos = gui.pos;
                    MutableComponent blockName = block.getName();

                    RenderUtils.drawString("Tick: " + ClientPlayerTickHandler.getCurrentHandlerTime(), x, y, Color.CYAN.getRGB(), true);
                    y += 9;
                    RenderUtils.drawString("类型: " + handler.getId(), x, y, Color.CYAN.getRGB(), true);
                    y += 9;
                    RenderUtils.drawString("位置: " + blockPos.toShortString(), x, y, Color.CYAN.getRGB(), true);
                    y += 9;
                    RenderUtils.drawString("方块: " + blockName.getString(), x, y, Color.CYAN.getRGB(), true);
                    y += 9;
                    RenderUtils.drawString("可以交互: " + gui.interacted, x, y, Color.CYAN.getRGB(), true);
                    y += 9;
                    RenderUtils.drawString("渲染范围: " + gui.posInSelectionRange, x, y, Color.CYAN.getRGB(), true);
                    y += 9;
                    RenderUtils.drawString("已经执行: " + gui.execute, x, y, Color.CYAN.getRGB(), true);

                    x += 50;    // 添加另一列偏移量
                }
            }
        }

        if (Configs.Core.LAG_CHECK.getBooleanValue() && Printer.getInstance().packetTick > 20) {
            RenderUtils.drawString("延迟过大，已暂停运行", (int) (width / 2), (int) (height / 2 - 22), Color.ORANGE.getRGB(), true, true);
        }
        if (Configs.Core.WORK_MODE.getOptionListValue().equals(ModeType.SINGLE)) {
            RenderUtils.drawString((int) (Printer.getInstance().getProgress() * 100) + "%", (int) (width / 2), (int) (height / 2 + 22), Color.WHITE.getRGB(), true, true);
            //#if MC > 11904
            guiGraphics.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
            guiGraphics.fill((int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getInstance().getProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
            //#else
            //$$ GuiComponent.fill(poseStack, (int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 + 20), (int) (height / 2 + 42), new Color(0, 0, 0, 150).getRGB());
            //$$ GuiComponent.fill(poseStack, (int) (width / 2 - 20), (int) (height / 2 + 36), (int) (width / 2 - 20 + Printer.getInstance().getProgress() * 40), (int) (height / 2 + 42), new Color(0, 255, 0, 255).getRGB());
            //#endif
        }
        RenderUtils.drawString(Configs.Core.WORK_MODE_TYPE.getOptionListValue().getDisplayName(), (int) (width / 2), (int) (height / 2 + 52), Color.WHITE.getRGB(), true, true);
        if (context != null) {
            RenderUtils.drawString(context.requiredState.getBlock().getName().getString(), (int) (width / 2), (int) (height / 2 + 64), Color.WHITE.getRGB(), true, true);
        }
    }

}
