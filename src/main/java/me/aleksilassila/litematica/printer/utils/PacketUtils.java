package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.PlayerLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class PacketUtils {

    private static final Minecraft client = Minecraft.getInstance();

    public static void sendPacket(Packet<?> packet) {
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            connection.send(packet);
        }
    }

    public static void sendPacket(MultiPlayerGameModeExtension.PredictiveAction packetCreator) {
        if (client.level instanceof SequenceExtension sequenceExtension) {
            int currentSequence = sequenceExtension.litematica_printer3$getSequence();
            Packet<ServerGamePacketListener> packet = packetCreator.predict(currentSequence);
            PacketUtils.sendPacket(packet);
        }
    }

    public static void sendLookPacket(LocalPlayer playerEntity, float lookYaw, float lookPitch) {
        playerEntity.connection.send(new ServerboundMovePlayerPacket.Rot(
                lookYaw,
                lookPitch,
                playerEntity.onGround()
                //#if MC > 12101
                , playerEntity.horizontalCollision
                //#endif
        ));
    }

    public static void sendLookPacket(LocalPlayer playerEntity, PlayerLook playerLook) {
        sendLookPacket(playerEntity, playerLook.yaw(), playerLook.pitch());
    }

    public interface SequenceExtension {
        default int litematica_printer3$getSequence() {
            return 0;
        }
    }
}