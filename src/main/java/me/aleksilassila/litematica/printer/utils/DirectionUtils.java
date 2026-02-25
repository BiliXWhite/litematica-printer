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
        float sinPitch = sin(pitchRad);
        float cosPitch = cos(pitchRad);
        float sinYaw = sin(yawRad);
        float cosYaw = cos(yawRad);
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
        float blockFrontYaw = rotation * 22.5F;
        float playerLookYaw = blockFrontYaw + 180.0F;
        playerLookYaw = playerLookYaw % 360.0F;
        return playerLookYaw > 180.0F ? playerLookYaw - 360.0F : playerLookYaw;
    }
}