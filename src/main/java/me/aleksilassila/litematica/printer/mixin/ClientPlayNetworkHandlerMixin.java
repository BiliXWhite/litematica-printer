package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientConnection connection;

    @Shadow
    private MinecraftClient client;

    @Overwrite
    public void sendPacket(Packet<?> packet) {
        if (Printer.getPrinter() == null) {
            this.connection.send(packet);
            return;
        }

        Direction direction = Printer.getPrinter().queue.lookDir;

        if (direction != null && Implementation.isLookAndMovePacket(packet)) {
            Packet<?> fixedPacket = Implementation.getFixedLookPacket(client.player, packet, direction);

            if (fixedPacket != null) {
                this.connection.send(fixedPacket);
            }
        } else if (direction == null || !Implementation.isLookOnlyPacket(packet)) {
            this.connection.send(packet);
        }
    }
}
