package me.aleksilassila.litematica.printer.utils.minecraft;

import me.aleksilassila.litematica.printer.printer.PlayerLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NetworkUtils {

    private static final Minecraft client = Minecraft.getInstance();
    private static final ThreadLocal<Boolean> BYPASS_QUEUED_LOOK_OVERRIDE = ThreadLocal.withInitial(() -> false);
    private static PlayerLook scopedLookOverride;

    public static void sendPacket(Packet<?> packet) {
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            connection.send(packet);
        }
    }

    public static void sendPacket(PredictiveAction packetCreator) {
        if (client.level instanceof SequenceExtension sequenceExtension) {
            Packet<ServerGamePacketListener> packet = sequenceExtension.litematica_printer3$predict(packetCreator);
            NetworkUtils.sendPacket(packet);
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
        sendLookPacket(playerEntity, playerLook.getYaw(), playerLook.getPitch());
    }

    public static void sendLookPacketIgnoringQueuedLook(LocalPlayer playerEntity, float lookYaw, float lookPitch) {
        try {
            BYPASS_QUEUED_LOOK_OVERRIDE.set(true);
            sendLookPacket(playerEntity, lookYaw, lookPitch);
        } finally {
            BYPASS_QUEUED_LOOK_OVERRIDE.set(false);
        }
    }

    public static void sendLookPacketIgnoringQueuedLook(LocalPlayer playerEntity, PlayerLook playerLook) {
        sendLookPacketIgnoringQueuedLook(playerEntity, playerLook.getYaw(), playerLook.getPitch());
    }

    public static boolean shouldBypassQueuedLookOverride() {
        return BYPASS_QUEUED_LOOK_OVERRIDE.get();
    }

    public static void setScopedLookOverride(PlayerLook playerLook) {
        scopedLookOverride = playerLook;
    }

    public static void clearScopedLookOverride() {
        scopedLookOverride = null;
    }

    public static PlayerLook getScopedLookOverride() {
        return scopedLookOverride;
    }

    public interface SequenceExtension {
        default Packet<ServerGamePacketListener> litematica_printer3$predict(PredictiveAction action) {
            return action.predict(0);
        }
    }
}
