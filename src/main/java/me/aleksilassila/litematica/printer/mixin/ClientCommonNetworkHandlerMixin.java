package me.aleksilassila.litematica.printer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.Printer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.cancelMovePack;

//#if MC > 12001
import net.minecraft.client.network.ClientCommonNetworkHandler;
@Mixin(value = ClientCommonNetworkHandler.class)
//#else
//$$ import net.minecraft.client.network.ClientPlayNetworkHandler;
//$$ @Mixin(ClientPlayNetworkHandler.class)
//#endif
public class ClientCommonNetworkHandlerMixin {
    @Final
    @Shadow
    protected ClientConnection connection;

    @Final
    @Shadow
    protected MinecraftClient client;

    /**
     * @author 6
     * @reason 6
     */
//    @Overwrite
    //#if MC < 12004
    //$$ @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V"),method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    //#else
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V"),method = "sendPacket")
    //#endif
    public void sendPacket(ClientConnection instance, Packet<?> packet, Operation<Void> original) {
        Direction direction = Printer.getPrinter().queue.lookDir;
        if (direction != null && Implementation.isLookAndMovePacket(packet)) {
            Packet<?> fixedPacket = Implementation.getFixedLookPacket(client.player, packet, direction);
            if (fixedPacket != null) {
                this.connection.send(fixedPacket);
                return;
            }
        } else if (direction == null || !Implementation.isLookOnlyPacket(packet)) {
            this.connection.send(packet);
            return;
        }
        original.call(instance, packet);
    }
}
