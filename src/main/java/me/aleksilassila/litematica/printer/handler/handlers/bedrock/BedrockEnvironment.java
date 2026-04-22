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
                && (isReplaceableOrResidue(level, torchPos) || isRedstoneTorch(level.getBlockState(torchPos)));
    }

    public static boolean isSlimeSupportUsable(ClientLevel level, BlockPos slimePos) {
        if (slimePos == null) {
            return false;
        }
        BlockPos torchPos = slimePos.above();
        BlockState slimeState = level.getBlockState(slimePos);
        return (slimeState.is(Blocks.SLIME_BLOCK) || isReplaceableOrResidue(level, slimePos))
                && (isReplaceableOrResidue(level, torchPos) || isRedstoneTorch(level.getBlockState(torchPos)));
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
            if (isReplaceableOrResidue(level, slimePos) && isReplaceableOrResidue(level, torchPos)) {
                return slimePos;
            }
        }
        return null;
    }

    public static boolean hasRoomForPiston(ClientLevel level, BlockPos bedrockPos) {
        BlockPos pistonPos = bedrockPos.above();
        BlockPos headPos = pistonPos.above();
        return isReplaceableOrResidue(level, pistonPos) && isReplaceableOrResidue(level, headPos);
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

    private static boolean isRedstoneTorch(BlockState state) {
        return state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH);
    }

    private static boolean isReplaceableOrResidue(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return BlockUtils.isReplaceable(state) || BedrockTargetBlocks.isCleanupResidue(state);
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
