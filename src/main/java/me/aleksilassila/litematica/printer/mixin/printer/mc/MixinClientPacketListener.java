package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.BreakUtils;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import me.aleksilassila.litematica.printer.utils.QuickShulkerUtils;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    /** 维度切换检测：记录 respawn 前的维度（同一维度重生不会触发） */
    @Unique
    private ResourceKey<Level> litematica_printer$dimensionBeforeRespawn;

    @Inject(method = "handleSetHealth", at = @At("RETURN"))
    private void injectHealthUpdate(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (packet.getHealth() == 0 && Configs.Core.AUTO_DISABLE_PRINTER.getBooleanValue() && Configs.Core.WORK_SWITCH.getBooleanValue()) {
            MessageUtils.setOverlayMessage(I18n.AUTO_DISABLE_NOTICE.getName());
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
        }
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void onRespawnStart(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        litematica_printer$dimensionBeforeRespawn = level == null ? null : level.dimension();
    }

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void onRespawnEnd(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ResourceKey<Level> fromDimension = litematica_printer$dimensionBeforeRespawn;
        litematica_printer$dimensionBeforeRespawn = null;
        // 与死亡自动关闭共用「自动关闭打印机」开关与提示
        if (fromDimension == null
                || !Configs.Core.AUTO_DISABLE_PRINTER.getBooleanValue()
                || !Configs.Core.WORK_SWITCH.getBooleanValue()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || fromDimension.equals(level.dimension())) return;
        // 维度切换：丢弃旧维度残留的待执行放置，避免切换到新维度后误放
        ActionManager.INSTANCE.clearQueue();
        MessageUtils.setOverlayMessage(I18n.AUTO_DISABLE_NOTICE.getName());
        Configs.Core.WORK_SWITCH.setBooleanValue(false);
    }

    @Inject(method = "handleContainerContent", at = @At("RETURN"))
    private void onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (QuickShulkerUtils.isOpenHandler() && player != null
                //#if MC > 12104
                && packet.containerId() == player.containerMenu.containerId
                //#else
                //$$ && packet.getContainerId() == player.containerMenu.containerId
                //#endif
        ) {
            QuickShulkerUtils.switchFromShulker();
        }
    }
}
