package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.Printer.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem.reSwitchItem;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    @Inject(at = @At("TAIL"),method = "onInventory")
    public void onInventory(InventoryS2CPacket packet, CallbackInfo ci){
         MinecraftClient mc = MinecraftClient.getInstance();
        if(isOpenHandler){
            Printer.getPrinter().switchInv();
        }
        if(reSwitchItem != null ){
            SwitchItem.reSwitchItem();
        }

        if (client.player != null && printerMemoryAdding) {
            client.player.closeHandledScreen();
        }
//        if(QuickShulkerUtils.waitForTheItemToBeSwitched != null) QuickShulkerUtils.switchItem(QuickShulkerUtils.targetSlot);
        if(num == 1 || num == 3)ZxyUtils.syncInv();
     }
}
