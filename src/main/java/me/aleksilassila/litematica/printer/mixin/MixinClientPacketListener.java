package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import me.aleksilassila.litematica.printer.utils.mods.ChestTrackerBridge;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.MenuType;
//#if MC >= 12103
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/MenuScreens;create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V"), method = "handleOpenScreen")
    private void printer$trackQuickShulkerContainerOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        QuickShulkerBridge.onContainerOpen(packet.getContainerId(), packet.getType());
        RuntimeAccess.get().containerSync().opened(packet.getContainerId(), packet.getType());
    }

    @Inject(
            method = "handleOpenScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/MenuScreens;create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V"
            ),
            cancellable = true
    )
    private void suppressTaskAnvilScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (packet.getType() != MenuType.ANVIL
                || RuntimeAccess.get().actionBroker().consumeManualAnvilScreenAllowance()) {
            return;
        }
        if (RuntimeAccess.get().actionBroker().consumeTaskAnvilScreenSuppression()) {
            NetworkUtils.sendPacket(new ServerboundContainerClosePacket(packet.getContainerId()));
            ci.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "handleOpenScreen")
    private void printer$trackContainerOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        ChestTrackerBridge.onContainerOpen(packet.getContainerId());
    }

    @Inject(at = @At("TAIL"), method = "handleContainerContent")
    public void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        //#if MC >= 12105
        int containerId = packet.containerId();
        //#else
        //$$ int containerId = packet.getContainerId();
        //#endif
        //#if MC >= 12105
        var items = packet.items();
        var carried = packet.carriedItem();
        //#else
        //$$ var items = packet.getItems();
        //$$ var carried = packet.getCarriedItem();
        //#endif
        for (int slot = 0; slot < items.size(); slot++) {
            QuickShulkerBridge.onSlotUpdate(containerId, slot, items.get(slot));
        }
        if (client.player != null && containerId == client.player.containerMenu.containerId) {
            QuickShulkerBridge.onSlotUpdate(-1, -1, carried);
        }
        ChestTrackerBridge.onContainerContent(containerId);
        if (client.player == null || containerId != client.player.containerMenu.containerId) {
            return;
        }
        QuickShulkerBridge.onInventoryContent(containerId, items, carried);
        //#if MC >= 12105
        RuntimeAccess.get().containerSync().content(containerId, packet.stateId(), items, carried);
        //#else
        //$$ RuntimeAccess.get().containerSync().content(containerId, packet.getStateId(), items, carried);
        //#endif
    }
    @Inject(at = @At("TAIL"), method = "handleContainerSetSlot")
    private void printer$confirmQuickShulkerSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        QuickShulkerBridge.onSlotUpdate(packet.getContainerId(), packet.getSlot(), packet.getItem());
    }
    //#if MC >= 12103
    @Inject(at = @At("TAIL"), method = "handleSetCursorItem")
    private void printer$trackCursor(ClientboundSetCursorItemPacket packet, CallbackInfo ci) {
        QuickShulkerBridge.onSlotUpdate(-1, -1, packet.contents());
    }
    //#endif
    @Inject(at = @At("TAIL"), method = "handleBlockUpdate")
    private void printer$logBlockReply(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        QuickShulkerDiagnostics.blockReply(packet.getPos(), packet.getBlockState());
    }

    @Inject(at = @At("TAIL"), method = "handleChunkBlocksUpdate")
    private void printer$logSectionReply(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        if (QuickShulkerDiagnostics.enabled()) packet.runUpdates(QuickShulkerDiagnostics::blockReply);
    }
}
