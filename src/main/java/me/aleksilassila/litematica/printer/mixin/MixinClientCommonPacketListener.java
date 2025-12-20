package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.interfaces.Implementation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//#if MC > 12001
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
@Mixin(value = ClientCommonPacketListenerImpl.class)
//#else
//$$ import net.minecraft.client.multiplayer.ClientPacketListener;
//$$ @Mixin(value = ClientPacketListener.class)
//#endif
public class MixinClientCommonPacketListener {
    @Final
    @Shadow
    //#if MC > 11802
    protected Connection connection;
    //#else
    //$$ private Connection connection;
    //#endif

    @Final
    @Shadow
    //#if MC > 11802
    protected Minecraft minecraft;
    //#else
    //$$ private Minecraft minecraft;
    //#endif

    /**
     * @author BiliXWhite
     * @reason 修改移动视角数据包，以实现欺骗服务器的效果
     */
    //#if MC < 12004
    //$$ @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"), method = "send(Lnet/minecraft/network/protocol/Packet;)V")
    //#else
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"), method = "send")
    //#endif
    private Packet<?> modifySendPacket(Packet<?> packet) {
        return Implementation.getFixedPacket(packet);
    }
}
