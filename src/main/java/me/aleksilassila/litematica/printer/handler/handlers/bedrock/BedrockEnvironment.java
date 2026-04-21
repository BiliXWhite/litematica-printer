package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.minecraft.BlockUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public final class BedrockEnvironment {
    private BedrockEnvironment() {
    }

    public static boolean isTorchSupportUsable(ClientLevel level, BlockPos supportPos) {
        if (supportPos == null) {
            return false;
        }
        BlockPos torchPos = supportPos.above();
        return BlockUtils.canSupportCenter(level, supportPos, Direction.UP)
                && (BlockUtils.isReplaceable(level.getBlockState(torchPos)) || level.getBlockState(torchPos).is(Blocks.REDSTONE_TORCH));
    }

    public static boolean isSlimeSupportUsable(ClientLevel level, BlockPos slimePos) {
        if (slimePos == null) {
            return false;
        }
        BlockPos torchPos = slimePos.above();
        return (level.getBlockState(slimePos).is(Blocks.SLIME_BLOCK) || BlockUtils.isReplaceable(level.getBlockState(slimePos)))
                && (BlockUtils.isReplaceable(level.getBlockState(torchPos)) || level.getBlockState(torchPos).is(Blocks.REDSTONE_TORCH));
    }

    public static BlockPos findTorchSupport(ClientLevel level, BlockPos bedrockPos) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            BlockPos support = bedrockPos.relative(direction);
            if (isTorchSupportUsable(level, support)) {
                return support;
            }
        }
        return null;
    }

    public static BlockPos findPossibleSlimeSupport(ClientLevel level, BlockPos bedrockPos) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            BlockPos slimePos = bedrockPos.relative(direction);
            BlockPos torchPos = slimePos.above();
            if (BlockUtils.isReplaceable(level.getBlockState(slimePos)) && BlockUtils.isReplaceable(level.getBlockState(torchPos))) {
                return slimePos;
            }
        }
        return null;
    }

    public static boolean hasRoomForPiston(ClientLevel level, BlockPos bedrockPos) {
        BlockPos pistonPos = bedrockPos.above();
        BlockPos headPos = pistonPos.above();
        return BlockUtils.isReplaceable(level.getBlockState(pistonPos)) && BlockUtils.isReplaceable(level.getBlockState(headPos));
    }

    public static List<BlockPos> findNearbyRedstoneTorches(ClientLevel level, BlockPos pistonPos) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos candidate : getTorchInfluencePositions(pistonPos)) {
            if (level.getBlockState(candidate).is(Blocks.REDSTONE_TORCH)) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static List<BlockPos> getTorchInfluencePositions(BlockPos pistonPos) {
        List<BlockPos> result = new ArrayList<>();
        for (int yOffset : new int[]{0, 1, -1}) {
            BlockPos center = pistonPos.offset(0, yOffset, 0);
            for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
                result.add(center.relative(direction));
            }
        }
        return result;
    }
}
