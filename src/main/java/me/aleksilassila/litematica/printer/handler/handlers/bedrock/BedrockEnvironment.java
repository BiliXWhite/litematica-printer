package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.minecraft.BlockUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
                && (BlockUtils.isReplaceable(level.getBlockState(torchPos)) || isRedstoneTorch(level.getBlockState(torchPos)));
    }

    public static boolean isSlimeSupportUsable(ClientLevel level, BlockPos slimePos) {
        if (slimePos == null) {
            return false;
        }
        BlockPos torchPos = slimePos.above();
        return (level.getBlockState(slimePos).is(Blocks.SLIME_BLOCK) || BlockUtils.isReplaceable(level.getBlockState(slimePos)))
                && (BlockUtils.isReplaceable(level.getBlockState(torchPos)) || isRedstoneTorch(level.getBlockState(torchPos)));
    }

    public static BlockPos findTorchSupport(ClientLevel level, BlockPos bedrockPos) {
        return findTorchSupport(level, bedrockPos, null);
    }

    public static BlockPos findTorchSupport(ClientLevel level, BlockPos centerPos, Direction excludedAxis) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            if (excludedAxis != null && direction == excludedAxis) {
                continue;
            }
            BlockPos support = centerPos.relative(direction);
            if (isTorchSupportUsable(level, support)) {
                return support;
            }
        }
        return null;
    }

    public static BlockPos findPossibleSlimeSupport(ClientLevel level, BlockPos bedrockPos) {
        return findPossibleSlimeSupport(level, bedrockPos, null);
    }

    public static BlockPos findPossibleSlimeSupport(ClientLevel level, BlockPos centerPos, Direction excludedAxis) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            if (excludedAxis != null && direction == excludedAxis) {
                continue;
            }
            BlockPos slimePos = centerPos.relative(direction);
            BlockPos torchPos = slimePos.above();
            if (BlockUtils.isReplaceable(level.getBlockState(slimePos)) && BlockUtils.isReplaceable(level.getBlockState(torchPos))) {
                return slimePos;
            }
        }
        return null;
    }

    public static boolean hasRoomForPiston(ClientLevel level, BlockPos bedrockPos) {
        return hasRoomForPiston(level, bedrockPos.above(), Direction.UP);
    }

    public static boolean hasRoomForPiston(ClientLevel level, BlockPos pistonPos, Direction facing) {
        BlockPos headPos = pistonPos.relative(facing);
        return BlockUtils.isReplaceable(level.getBlockState(pistonPos)) && BlockUtils.isReplaceable(level.getBlockState(headPos));
    }

    public static List<BlockPos> findNearbyRedstoneTorches(ClientLevel level, BlockPos pistonPos) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos candidate : getTorchInfluencePositions(pistonPos)) {
            if (isRedstoneTorch(level.getBlockState(candidate))) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static boolean isRedstoneTorch(BlockState state) {
        return state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH);
    }

    public static boolean isRedstoneTorchAt(ClientLevel level, BlockPos pos) {
        return pos != null && isRedstoneTorch(level.getBlockState(pos));
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
