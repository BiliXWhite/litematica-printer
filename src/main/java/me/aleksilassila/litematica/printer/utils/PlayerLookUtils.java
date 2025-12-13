package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.interfaces.Implementation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;

public class PlayerLookUtils {
    private static boolean modifyYaw = false;
    private static boolean modifyPitch = false;
    private static boolean modifyOnGround = false;
    private static float yaw = 0F;
    private static float pitch = 0F;
    private static boolean onGround = false;

    public static float getYaw(float yaw) {
        return PlayerLookUtils.modifyYaw ? PlayerLookUtils.yaw : yaw;
    }

    public static float getPitch(float pitch) {
        return PlayerLookUtils.modifyPitch ? PlayerLookUtils.pitch : pitch;
    }

    public static boolean getOnGround(boolean onGround) {
        return PlayerLookUtils.modifyOnGround ? PlayerLookUtils.onGround : onGround;
    }

    public static boolean getHasRot(boolean hasRot) {
        return isModifying() || hasRot;
    }

    public static boolean isModifying() {
        return modifyYaw || modifyPitch || modifyOnGround;
    }

    public static void setYaw(float yaw) {
        PlayerLookUtils.yaw = yaw;
        PlayerLookUtils.modifyYaw = true;
    }

    public static void setPitch(float pitch) {
        PlayerLookUtils.pitch = pitch;
        PlayerLookUtils.modifyPitch = true;
    }

    public static void setOnGround(boolean onGround) {
        PlayerLookUtils.onGround = onGround;
        PlayerLookUtils.modifyOnGround = true;
    }

    public static void setFull(float yaw, float pitch, boolean onGround) {
        PlayerLookUtils.setYaw(yaw);
        PlayerLookUtils.setPitch(pitch);
        PlayerLookUtils.setOnGround(onGround);
    }

    public static void resetYaw() {
        PlayerLookUtils.modifyYaw = false;
    }

    public static void resetPitch() {
        PlayerLookUtils.modifyPitch = false;
    }

    public static void resetOnGround() {
        PlayerLookUtils.modifyOnGround = true;
    }

    public static void restFull() {
        PlayerLookUtils.resetYaw();
        PlayerLookUtils.resetPitch();
        PlayerLookUtils.resetOnGround();
    }

    // 根据当前视角(如果正在修改, 则会使用正在修改的视角)
    public static Direction getPlacementDirection() {
        float currentYaw = getYaw(player != null ? player.getYRot() : 0F);
        float currentPitch = getPitch(player != null ? player.getXRot() : 0F);
        return DirectionUtils.orderedByNearest(currentYaw, currentPitch)[0].getOpposite();
    }

    public static ServerboundMovePlayerPacket getLookPacket(float yaw, float pitch) {
        //#if MC > 12101
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), false);
        //#else
        //$$ return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround());
        //#endif
    }

    public static void sendLookPacket(LocalPlayer playerEntity) {
        playerEntity.connection.send(getLookPacket(playerEntity.getYRot(), playerEntity.getXRot()));
    }
}
