package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.Printer.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils.*;


@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    @Shadow @Final private MinecraftClient client;


//    /**
//     * @author 2
//     * @reason 2
//     */
//    @Overwrite
//    public void onOpenScreen(OpenScreenS2CPacket packet) {
//        NetworkThreadUtils.forceMainThread(packet, client.getNetworkHandler(), client);
//        if(isOpenHandler){
//            System.out.println("skip");
//            this.packet = packet;
//            return;
//        }
//        HandledScreens.open(packet.getScreenHandlerType(), client, packet.getSyncId(), packet.getName());
//    }
    /**
     * @author 2
     * @reason 2
     */
//    @Overwrite
     @Inject(at = @At("TAIL"),method = "onInventory")
    public void onInventory(InventoryS2CPacket packet, CallbackInfo ci){
         MinecraftClient mc = MinecraftClient.getInstance();
        if(isOpenHandler){
            Printer.getPrinter().switchInv();
        }
         if(adding && mc.player != null){
             mc.player.closeHandledScreen();
         }
         if(num>1){
             ZxyUtils.syncInv();
         }

     }
}
