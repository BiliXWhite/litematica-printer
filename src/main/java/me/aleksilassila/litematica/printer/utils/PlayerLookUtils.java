package me.aleksilassila.litematica.printer.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class PlayerLookUtils {
    static Minecraft minecraft = Minecraft.getInstance();
    private static boolean modifyYaw = false;
    private static boolean modifyPitch = false;
    private static float yaw = 0F;
    private static float pitch = 0F;

    public static float getYaw(float yaw) {
        return PlayerLookUtils.modifyYaw ? PlayerLookUtils.yaw : yaw;
    }

    public static float getPitch(float pitch) {
        return PlayerLookUtils.modifyPitch ? PlayerLookUtils.pitch : pitch;
    }

    public static boolean getHasRot(boolean hasRot) {
        return isModifying() || hasRot;
    }

    public static boolean isModifying() {
        return modifyYaw || modifyPitch;
    }

    public static void setYaw(float yaw) {
        PlayerLookUtils.yaw = yaw;
        PlayerLookUtils.modifyYaw = true;
    }

    public static void setPitch(float pitch) {
        PlayerLookUtils.pitch = pitch;
        PlayerLookUtils.modifyPitch = true;
    }

    public static void setDirection(Direction direction) {
        setPitch(getRequiredYaw(direction));
        setPitch(getRequiredPitch(direction));
    }

    public static boolean isModifyYaw() {
        return modifyYaw;
    }

    public static boolean isModifyPitch() {
        return modifyPitch;
    }

    public static void setFull(float yaw, float pitch) {
        PlayerLookUtils.setYaw(yaw);
        PlayerLookUtils.setPitch(pitch);
    }

    public static void resetYaw() {
        PlayerLookUtils.modifyYaw = false;
    }

    public static void resetPitch() {
        PlayerLookUtils.modifyPitch = false;
    }

    public static void resetFull() {
        PlayerLookUtils.resetYaw();
        PlayerLookUtils.resetPitch();
    }

    // 根据当前视角(如果正在修改, 则会使用正在修改的视角)
    public static Direction getPlacementDirection() {
        LocalPlayer player = minecraft.player;
        float currentYaw = getYaw(player != null ? player.getYRot() : 0F);
        float currentPitch = getPitch(player != null ? player.getXRot() : 0F);
        return DirectionUtils.orderedByNearest(currentYaw, currentPitch)[0].getOpposite();
    }

    public static ServerboundMovePlayerPacket getLookPacket(float yaw, float pitch) {
        LocalPlayer player = minecraft.player;
        boolean onGround = player == null;
        //#if MC > 12101
        return new ServerboundMovePlayerPacket.Rot(getYaw(yaw), getPitch(pitch), onGround, false);
        //#else
        //$$ return new ServerboundMovePlayerPacket.Rot(getYaw(yaw), getPitch(pitch), onGround);
        //#endif
    }


    public static void sendLookPacket(LocalPlayer playerEntity, float yaw, float pitch) {
        playerEntity.connection.send(getLookPacket(yaw, pitch));
    }


    public static void sendLookPacket(LocalPlayer playerEntity) {
        sendLookPacket(playerEntity, playerEntity.getYRot(), playerEntity.getXRot());
    }

    /**
     * 根据方向获取对应的水平旋转角（偏航角Yaw）
     *
     * @param playerShouldBeFacing 目标方向
     * @return 偏航角（水平旋转角度，范围0-360°）：仅水平方向有效，非水平方向返回0
     */
    public static float getRequiredYaw(Direction playerShouldBeFacing) {
        // 判断方向是否为水平轴（东/西/南/北）
        if (playerShouldBeFacing.getAxis().isHorizontal()) {
            // 将方向转换为对应的偏航角（如东=90°，南=180°）
            return playerShouldBeFacing.toYRot();
        } else {
            // 垂直方向（上/下）返回0°偏航角
            return 0;
        }
    }

    /**
     * 根据方向获取对应的垂直旋转角（俯仰角Pitch）
     *
     * @param playerShouldBeFacing 目标方向
     * @return 俯仰角（垂直旋转角度，范围-90°到90°）：仅垂直方向有效，非垂直方向返回0
     */
    public static float getRequiredPitch(Direction playerShouldBeFacing) {
        // 判断方向是否为垂直轴（上/下）
        if (playerShouldBeFacing.getAxis().isVertical()) {
            // 向下=90°，向上=-90°（Minecraft的俯仰角定义）
            return playerShouldBeFacing == Direction.DOWN ? 90 : -90;
        } else {
            // 水平方向返回0°俯仰角
            return 0;
        }
    }
}
