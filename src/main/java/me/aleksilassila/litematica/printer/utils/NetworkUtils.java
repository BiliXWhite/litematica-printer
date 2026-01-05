package me.aleksilassila.litematica.printer.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import org.jetbrains.annotations.Nullable;

public class NetworkUtils {

    private static final Minecraft client = Minecraft.getInstance();

    public static void sendPacket(Packet<?> packet) {
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            connection.send(packet);
        }
    }

    public static void sendPacket(Packet<?> packet, @Nullable Runnable beforeSending, @Nullable Runnable afterSending) {
        if (beforeSending != null) {
            beforeSending.run();
        }
        NetworkUtils.sendPacket(packet);
        if (afterSending != null) {
            afterSending.run();
        }
    }

    public static void sendSequencedPacket(PredictiveAction packetCreator, @Nullable Runnable beforeSending, @Nullable Runnable afterSending) {
        if (client.level instanceof SequenceExtension sequenceExtension) {
            int currentSequence = sequenceExtension.litematica_printer3$getSequence();
            Packet<ServerGamePacketListener> packet = packetCreator.predict(currentSequence);
            NetworkUtils.sendPacket(packet, beforeSending, afterSending);
        }
    }

    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        NetworkUtils.sendSequencedPacket(packetCreator, null, null);
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface PredictiveAction {
        Packet<ServerGamePacketListener> predict(int sequence);
    }

    public interface SequenceExtension {
        default int litematica_printer3$getSequence() {
            return 0;
        }
    }
}