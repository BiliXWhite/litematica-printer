package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class DirectionUtils {

    /**
     * 根据指定的偏航角(yaw)和俯仰角(pitch)，返回按最接近视线方向排序的方向数组
     *
     * @param yaw 偏航角（角度制），0度为正南，逆时针增加
     * @param pitch 俯仰角（角度制），-90度为向上看，90度为向下看
     * @return 按最接近视线方向排序的6个方向数组
     */
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

    /**
     * 创建一个包含三个方向及其相反方向的方向数组
     *
     * @param dir1 第一个方向
     * @param dir2 第二个方向
     * @param dir3 第三个方向
     * @return 包含6个方向的数组，顺序为：dir1, dir2, dir3, dir3的反方向, dir2的反方向, dir1的反方向
     */
    private static Direction[] makeDirectionArray(Direction dir1, Direction dir2, Direction dir3) {
        return new Direction[]{dir1, dir2, dir3, dir3.getOpposite(), dir2.getOpposite(), dir1.getOpposite()};
    }
}