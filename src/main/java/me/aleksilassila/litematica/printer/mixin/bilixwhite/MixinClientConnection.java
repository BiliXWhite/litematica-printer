package me.aleksilassila.litematica.printer.mixin.bilixwhite;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    /**
     * @author BiliXWhite
     * @reason 用于延迟检测
     */
    @Inject(method = "handlePacket", at = @At("HEAD"), require = 1)
    private static void hookReceivingPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        // 减少性能开销
        if ((!LitematicaMixinMod.PRINT_SWITCH.getBooleanValue() && !LitematicaMixinMod.PRINT.getKeybind().isPressed()) || !LitematicaMixinMod.LAG_CHECK.getBooleanValue())
            return;
        Printer.packetTick = 0;
    }
}
