package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.printerMemoryAdding;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Shadow @Final protected MinecraftClient client;
    @Inject(at = @At("HEAD"), method = "closeScreen")
    public void closeScreen(CallbackInfo ci) {
        BlockPos pos = MemoryUtils.getLatestPos();
        if(Statistics.loadChestTracker && LitematicaPrinterMod.CLOUD_INVENTORY.getBooleanValue() &&
                (LitematicaPrinterMod.PRINT_SWITCH.getBooleanValue() || LitematicaPrinterMod.PRINT.getKeybind().isPressed() || printerMemoryAdding || syncPrinterInventory) &&(
                pos != null || MemoryUtils.getMemoryPos() != null)){
            if(!client.player.currentScreenHandler.equals(client.player.playerScreenHandler)){
                MemoryUtils.handleItemsFromScreen(client.player.currentScreenHandler);
            }
        }
        OpenInventoryPacket.reSet();
    }
}
