package me.aleksilassila.litematica.printer.utils.minecraft;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Util;

@SuppressWarnings("SpellCheckingInspection")
public class DirectionUtils {
    private static final float YAW_MIN = -180.0F;
    private static final float YAW_MAX = 180.0F;
    private static final int ROTATION_MIN = 0;
    private static final int ROTATION_MAX = 15;
    private static final float ROTATION_TO_YAW_FACTOR = 22.5F;

    public static float getRequiredYaw(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing != null && playerShouldBeFacing.getAxis().isHorizontal()) {
            return playerShouldBeFacing.toYRot();
        } else {
            return 0;
        }
    }

    public static float getRequiredPitch(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing != null && playerShouldBeFacing.getAxis().isVertical()) {
            return playerShouldBeFacing == Direction.DOWN ? 90 : -90;
        } else {
            return 0;
        }
    }

    public static Vec3i getVector(Direction direction) {
        //#if MC >= 12103
        return direction.getUnitVec3i();
        //#else
        //$$ return direction.getNormal();
        //#endif
    }

    public static Direction[] orderedByNearest(float yaw, float pitch) {
        double pitchRad = pitch * (Math.PI / 180.0);
        double yawRad = -yaw * (Math.PI / 180.0);
        float sinPitch = (float) Math.sin(pitchRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);
        boolean isEastFacing = sinYaw > 0.0F;
        boolean isUpFacing = sinPitch < 0.0F;
        boolean isSouthFacing = cosYaw > 0.0F;
        float eastWestMagnitude = isEastFacing ? sinYaw : -sinYaw;
        float upDownMagnitude = isUpFacing ? -sinPitch : sinPitch;
        float northSouthMagnitude = isSouthFacing ? cosYaw : -cosYaw;
        float adjustedX = eastWestMagnitude * cosPitch;
        float adjustedZ = northSouthMagnitude * cosPitch;
        Direction primaryXDirection = isEastFacing ? Direction.EAST : Direction.WEST;
        Direction primaryYDirection = isUpFacing ? Direction.UP : Direction.DOWN;
        Direction primaryZDirection = isSouthFacing ? Direction.SOUTH : Direction.NORTH;
        if (eastWestMagnitude > northSouthMagnitude) {
            if (upDownMagnitude > adjustedX) {
                return makeDirectionArray(primaryYDirection, primaryXDirection, primaryZDirection);
            } else {
                return adjustedZ > upDownMagnitude
                        ? makeDirectionArray(primaryXDirection, primaryZDirection, primaryYDirection)
                        : makeDirectionArray(primaryXDirection, primaryYDirection, primaryZDirection);
            }
        } else if (upDownMagnitude > adjustedZ) {
            return makeDirectionArray(primaryYDirection, primaryZDirection, primaryXDirection);
        } else {
            return adjustedX > upDownMagnitude
                    ? makeDirectionArray(primaryZDirection, primaryXDirection, primaryYDirection)
                    : makeDirectionArray(primaryZDirection, primaryYDirection, primaryXDirection);
        }
    }

    private static Direction[] makeDirectionArray(Direction dir1, Direction dir2, Direction dir3) {
        return new Direction[]{dir1, dir2, dir3, dir3.getOpposite(), dir2.getOpposite(), dir1.getOpposite()};
    }

    public static Direction getHorizontalDirection(float yaw) {
        return Direction.fromYRot(yaw);
    }


    public static float rotationToPlayerYaw(int rotation) {
        // 防范性处理：确保rotation在0-15范围内（即使传入非法值也能修正）
        rotation = clampRotation(rotation);
        float blockFrontYaw = rotation * ROTATION_TO_YAW_FACTOR;
        float playerLookYaw = blockFrontYaw + 180.0F;
        // 强制归一化到[-180, 180]（核心防范逻辑）
        playerLookYaw = normalizeYaw(playerLookYaw);
        return playerLookYaw;
    }

    public static int getOppositeRotation(int rotation) {
        // 先修正输入的Rotation范围
        rotation = clampRotation(rotation);
        float playerYaw = rotationToPlayerYaw(rotation);
        float oppositeYaw = getOppositeYaw(playerYaw);
        // 转换回Rotation前，先归一化Yaw，再计算
        float normalizedYaw = oppositeYaw < 0.0F ? oppositeYaw + 360.0F : oppositeYaw;
        float blockFrontYaw = normalizedYaw - 180.0F;
        if (blockFrontYaw < 0.0F) {
            blockFrontYaw += 360.0F;
        }
        int oppositeRotation = Math.round(blockFrontYaw / ROTATION_TO_YAW_FACTOR);
        // 最后再修正Rotation范围，双重保障
        oppositeRotation = clampRotation(oppositeRotation);
        return oppositeRotation;
    }

    public static float getOppositeYaw(float playerLookYaw) {
        // 先归一化输入的Yaw（即使传入非法值，比如500、-400也能修正）
        playerLookYaw = normalizeYaw(playerLookYaw);
        float oppositeYaw = playerLookYaw + 180.0F;
        // 再次归一化，确保输出在[-180, 180]
        oppositeYaw = normalizeYaw(oppositeYaw);
        return oppositeYaw;
    }

    private static float normalizeYaw(float yaw) {
        // 先取模360，将值约束到[-360, 360]
        yaw = yaw % 360.0F;
        // 再调整到[-180, 180]
        if (yaw > YAW_MAX) {
            yaw -= 360.0F;
        } else if (yaw < YAW_MIN) {
            yaw += 360.0F;
        }
        return yaw;
    }

    private static int clampRotation(int rotation) {
        // 取模16，将值约束到[-15, 15]
        rotation = rotation % (ROTATION_MAX + 1);
        // 处理负数，修正到0-15
        if (rotation < ROTATION_MIN) {
            rotation += (ROTATION_MAX + 1);
        }
        return rotation;
    }
}