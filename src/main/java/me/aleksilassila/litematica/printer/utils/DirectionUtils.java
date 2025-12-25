package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;

public class DirectionUtils {

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

    public static Direction getFacingAxisX(float yaw) {
        return Direction.EAST.isFacingAngle(yaw) ? Direction.EAST : Direction.WEST;
    }

    public static Direction getFacingAxisY(float pitch) {
        return pitch < 0.0F ? Direction.UP : Direction.DOWN;
    }

    public static Direction getFacingAxisZ(float yaw) {
        return Direction.SOUTH.isFacingAngle(yaw) ? Direction.SOUTH : Direction.NORTH;
    }

    public static Direction[] orderedByNearest(float yaw, float pitch) {
        // 将角度转换为弧度
        float pitchRad = pitch * (float) (Math.PI / 180.0);
        float yawRad = -yaw * (float) (Math.PI / 180.0);

        // 计算三角函数
        float sinPitch = Mth.sin(pitchRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinYaw = Mth.sin(yawRad);
        float cosYaw = Mth.cos(yawRad);

        // 判断方向的布尔标志
        boolean isEastFacing = sinYaw > 0.0F;
        boolean isUpFacing = sinPitch < 0.0F;
        boolean isSouthFacing = cosYaw > 0.0F;

        // 计算各方向分量的绝对值
        float eastWestMagnitude = isEastFacing ? sinYaw : -sinYaw;
        float upDownMagnitude = isUpFacing ? -sinPitch : sinPitch;
        float northSouthMagnitude = isSouthFacing ? cosYaw : -cosYaw;

        // 计算调整后的分量
        float adjustedX = eastWestMagnitude * cosPitch;
        float adjustedZ = northSouthMagnitude * cosPitch;

        // 确定基础方向
        Direction primaryXDirection = isEastFacing ? Direction.EAST : Direction.WEST;
        Direction primaryYDirection = isUpFacing ? Direction.UP : Direction.DOWN;
        Direction primaryZDirection = isSouthFacing ? Direction.SOUTH : Direction.NORTH;

        // 根据分量比较确定方向优先级
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
}