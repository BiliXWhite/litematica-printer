package me.aleksilassila.litematica.printer.mixin.bilixwhite.others;

import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinClientConnection {
/**
     * @author BiliXWhite
     * @reason 用于延迟检测
     */
    @Inject(method = "genericsFtw", at = @At("HEAD"), require = 1)
    private static void hookGenericsFtw(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        // 减少性能开销
        if ((!LitematicaPrinterMod.PRINT_SWITCH.getBooleanValue() && !LitematicaPrinterMod.PRINT.getKeybind().isPressed()) || !LitematicaPrinterMod.LAG_CHECK.getBooleanValue())
            return;
        Printer.packetTick = 0;
    }
}
