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

    public static boolean canInteractWithBlockAt(LocalPlayer player, BlockPos blockPos, double additionalRange) {
        double blockPosX = blockPos.getX();
        double blockPosY = blockPos.getY();
        double blockPosZ = blockPos.getZ();
        //#if MC >= 12005
        double distance = getPlayerBlockInteractionRange() + additionalRange;
        double eyePosX = player.getX();
        double eyePosY = player.getEyeY();
        double eyePosZ = player.getZ();
        double dx = Math.max(Math.max(blockPosX - eyePosX, eyePosX - (blockPosX + 1)), 0);
        double dy = Math.max(Math.max(blockPosY - eyePosY, eyePosY - (blockPosY + 1)), 0);
        double dz = Math.max(Math.max(blockPosZ - eyePosZ, eyePosZ - (blockPosZ + 1)), 0);
        return (dx * dx + dy * dy + dz * dz) < (distance * distance);
        //#elseif MC >= 11900
        //$$ double distance = Math.max(getPlayerBlockInteractionRange(), 5) + additionalRange;
        //$$ double eyePosX = player.getX();
        //$$ double eyePosY = player.getEyeY();
        //$$ double eyePosZ = player.getZ();
        //$$ double dx = eyePosX - (blockPosX + 0.5);
        //$$ double dy = eyePosY - (blockPosY + 0.5);
        //$$ double dz = eyePosZ - (blockPosZ + 0.5);
        //$$ return (dx * dx + dy * dy + dz * dz) < (distance * distance);
        //#else
        //$$ // MC <= 11802
        //$$ double distance = Math.max(getPlayerBlockInteractionRange(), 5) + additionalRange;
        //$$ double dx = player.getX() - (blockPosX + 0.5);
        //$$ double dy = player.getY() - (blockPosY + 0.5) + 1.5;
        //$$ double dz = player.getZ() - (blockPosZ + 0.5);
        //$$ return (dx * dx + dy * dy + dz * dz) < (distance * distance);
        //#endif
    }

    // 判断是否可交互
    public static boolean canInteracted(BlockPos blockPos) {
        double workRange = Configs.General.PRINTER_RANGE.getIntegerValue();
        if (Configs.General.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()) {
            if (client.player != null && !canInteractWithBlockAt(client.player, blockPos, 1F)) {
                return false;
            }
        }
        if (Configs.General.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType radiusShapeType) {
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
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) <= range * range;
    }

    // 八面体（曼哈顿距离）
    public static boolean canInteractedManhattan(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        BlockPos center = player.blockPosition();
        int dx = Math.abs(blockPos.getX() - center.getX());
        int dy = Math.abs(blockPos.getY() - center.getY());
        int dz = Math.abs(blockPos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }

    // 立方体（CUBE）：以玩家方块位置为中心
    public static boolean canInteractedCube(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        BlockPos center = player.blockPosition();
        int dx = Math.abs(blockPos.getX() - center.getX());
        int dy = Math.abs(blockPos.getY() - center.getY());
        int dz = Math.abs(blockPos.getZ() - center.getZ());
        return dx <= range && dy <= range && dz <= range;
    }
}
