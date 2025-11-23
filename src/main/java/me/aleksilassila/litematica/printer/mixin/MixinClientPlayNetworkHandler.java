package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem.reSwitchItem;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler {

    @Inject(at = @At("TAIL"),method = "handleContainerContent")
    public void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if(isOpenHandler){
            InventoryUtils.switchInv();
        }
        if(reSwitchItem != null ){
            SwitchItem.reSwitchItem();
        }

        if (Minecraft.getInstance().player != null && printerMemoryAdding) {
            Minecraft.getInstance().player.closeContainer();
        }
        if(num == 1 || num == 3)ZxyUtils.syncInv();
    }

    @Inject(method = "handleSetHealth", at = @At("RETURN"))
    private void injectHealthUpdate(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        if (packet.getHealth() == 0 && LitematicaPrinterMod.AUTO_DISABLE_PRINTER.getBooleanValue()) {
            ZxyUtils.actionBar(StringUtils.get("auto_disable_notice").getString());
            LitematicaPrinterMod.PRINT_SWITCH.setBooleanValue(false);
        }
    }
}
