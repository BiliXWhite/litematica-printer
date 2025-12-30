package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.RadiusShapeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

public class PlayerUtils {
    private static final Minecraft client = Minecraft.getInstance();

    public static Abilities getAbilities(LocalPlayer playerEntity) {
        return playerEntity.getAbilities();
    }

    public static double getPlayerBlockInteractionRange(double defaultRange) {
        //#if MC>=12005
        if (client.player != null) {
            return client.player.blockInteractionRange();
        }
        //#else
        //$$ if (client.gameMode != null) {
        //$$    return client.gameMode.getPickRange();
        //$$ }
        //#endif
        return defaultRange;
    }

    public static double getPlayerBlockInteractionRange() {
        return getPlayerBlockInteractionRange(4.5F);
    }

    // 判断是否可交互
    public static boolean canInteracted(BlockPos blockPos) {
        int workRange = Configs.PRINTER_RANGE.getIntegerValue();
        if (Configs.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType radiusShapeType) {
            return switch (radiusShapeType) {
                case SPHERE -> canInteractedEuclidean(blockPos, workRange);
                case OCTAHEDRON -> canInteractedManhattan(blockPos, workRange);
                case CUBE -> canInteractedCube(blockPos, workRange);
            };
        }
        return true;
    }

    // 球面（欧几里得距离）
    public static boolean canInteractedEuclidean(BlockPos blockPos, double range) {
        var player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) <= range * range;
    }

    // 八面体（曼哈顿距离）
    public static boolean canInteractedManhattan(BlockPos pos, int range) {
        if (client.player == null) return false;
        BlockPos center = client.player.blockPosition();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }

    // 立方体（CUBE）：以玩家方块位置为中心
    public static boolean canInteractedCube(BlockPos pos, int range) {
        var player = client.player;
        if (player == null || pos == null) return false;
        BlockPos center = player.blockPosition();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx <= range && dy <= range && dz <= range;
    }
}
