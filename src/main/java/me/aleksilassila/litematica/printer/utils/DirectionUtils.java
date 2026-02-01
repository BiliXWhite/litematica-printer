package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Util;

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

    private static final float[] SIN = Util.make(new float[65536], fs -> {
        for (int ix = 0; ix < fs.length; ix++) {
            fs[ix] = (float)Math.sin(ix / 10430.378350470453);
        }
    });

    public static float sin(double d) {
        return SIN[(int)((long)(d * 10430.378350470453) & 65535L)];
    }

    public static float cos(double d) {
        return SIN[(int)((long)(d * 10430.378350470453 + 16384.0) & 65535L)];
    }

    public static Direction[] orderedByNearest(float yaw, float pitch) {
        double pitchRad = pitch * (Math.PI / 180.0);
        double yawRad = -yaw * (Math.PI / 180.0);

        // 计算三角函数
        float sinPitch = sin(pitchRad);
        float cosPitch = cos(pitchRad);
        float sinYaw = sin(yawRad);
        float cosYaw = cos(yawRad);

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

    /**
     * 将站立告示牌0-15的旋转值转换为MC对应的Yaw角度（水平旋转角）
     * 0=南(0°)、1=西南偏南(22.5°)、2=西南(45°)、3=西南偏西(67.5°)、4=西(90°)
     * 5=西北偏西(112.5°)、6=西北(135°)、7=西北偏北(157.5°)、8=北(180°)
     * 9=东北偏北(202.5°)、10=东北(225°)、11=东北偏东(247.5°)、12=东(270°)
     * 13=东南偏东(292.5°)、14=东南(315°)、15=东南偏南(337.5°)
     */
    public static float rotationToPlayerYaw(int rotation) {
        // 先计算方块正面朝向的原始角度（0°=南，360°=南）
        float blockFrontYaw = rotation * 22.5F;
        // 玩家需要看向的角度 = 方块正面朝向 + 180°（反向）
        float playerLookYaw = blockFrontYaw + 180.0F;
        // 转换为MC标准的-180°~180°范围
        return playerLookYaw > 180.0F ? playerLookYaw - 360.0F : playerLookYaw;
    }
}