package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.OperationQueue;
import me.aleksilassila.litematica.printer.printer.SchematicSnapshot;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Inject(method = "handleSetHealth", at = @At("RETURN"))
    private void injectHealthUpdate(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (packet.getHealth() == 0 && Configs.Core.AUTO_DISABLE_PRINTER.getBooleanValue() && Configs.Core.WORK_SWITCH.getBooleanValue()) {
            MessageUtils.setOverlayMessage(I18n.AUTO_DISABLE_NOTICE.getName());
            Configs.Core.WORK_SWITCH.setBooleanValue(false);
        }
    }

    @Inject(method = "handleContainerContent", at = @At("RETURN"))
    private void onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (InventoryUtils.isOpenHandler()) {
            InventoryUtils.switchFromShulker();
        }
    }

    @Inject(method = "handleBlockUpdate", at = @At("RETURN"))
    private void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        if (!ConfigUtils.isPrinterEnable()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        BlockPos pos = packet.getPos();
        BlockState newState = packet.getBlockState();

        // 1. 检查是否在原理图区域内
        if (!SchematicSnapshot.INSTANCE.contains(pos)) return;

        // 2. 比对原理图——看是否需要修复
        BlockState requiredState = SchematicSnapshot.INSTANCE.getRequiredState(pos);
        if (requiredState == null || newState.equals(requiredState)) return;

        // 3. 标记为脏坐标，由 OperationQueue 在下一 tick 处理
        // 注意：不做 SELF 冷却跳过——BlockUpdate 是来自服务器的权威状态，
        // 即使打印机刚操作过此位置（如刚破坏了一个错误方块），也必须处理，
        // 否则服务器确认的空方块永远不会被标记为需要修复。
        // 冷却保护在 iterateBlocks 的 isOnCooldown 中仍有生效，防止扫描重复处理。
        OperationQueue.INSTANCE.markDirty(pos);
    }
}
