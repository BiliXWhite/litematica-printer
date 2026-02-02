package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.exitGameReSet;

@Environment(EnvType.CLIENT)
@Mixin(Connection.class)
public class MixinConnection {
    @Inject(method = "genericsFtw", at = @At("HEAD"), require = 1)
    private static void hookGenericsFtw(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (Printer.isEnable()) {
            Printer.getInstance().packetTick = 0;   // 用于延迟检测
        }
    }

    @Inject(method = "disconnect*", at = {@At("HEAD")})
    public void disconnect(Component ignored, CallbackInfo ci) {
        exitGameReSet();    // 退出重置
    }
}
