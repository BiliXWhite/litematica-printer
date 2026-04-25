package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.minecraft.BlockUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class BedrockEnvironment {
    private BedrockEnvironment() {
    }

    public static boolean isTorchPlacementUsable(ClientLevel level, BedrockTorchPlacement placement) {
        if (placement == null || placement.getSupportPos() == null || placement.getClickedFace() == null) {
            return false;
        }
        BlockPos supportPos = placement.getSupportPos();
        BlockPos torchPos = placement.getTorchPos();
        Direction clickedFace = placement.getClickedFace();
        if (torchPos == null) {
            return false;
        }
        boolean supportOk = clickedFace == Direction.UP
                ? BlockUtils.canSupportCenter(level, supportPos, Direction.UP)
                : level.getBlockState(supportPos).isFaceSturdy(level, supportPos, clickedFace);
        if (!supportOk) {
            return false;
        }
        BlockState torchState = level.getBlockState(torchPos);
        if (BlockUtils.isReplaceable(torchState)) {
            return true;
        }
        if (!isRedstoneTorch(torchState)) {
            return false;
        }
        if (clickedFace == Direction.UP) {
            return torchState.is(Blocks.REDSTONE_TORCH);
        }
        return torchState.is(Blocks.REDSTONE_WALL_TORCH)
                && torchState.getValue(RedstoneWallTorchBlock.FACING) == clickedFace;
    }

    public static boolean isTorchSupportUsable(ClientLevel level, BlockPos supportPos) {
        return isTorchPlacementUsable(level, new BedrockTorchPlacement(supportPos, Direction.UP));
    }

    public static boolean isSlimeSupportUsable(ClientLevel level, BlockPos slimePos) {
        if (slimePos == null) {
            return false;
        }
        BlockPos torchPos = slimePos.above();
        return (level.getBlockState(slimePos).is(Blocks.SLIME_BLOCK) || BlockUtils.isReplaceable(level.getBlockState(slimePos)))
                && (BlockUtils.isReplaceable(level.getBlockState(torchPos)) || isRedstoneTorch(level.getBlockState(torchPos)));
    }

    public static boolean isSlimePlacementUsable(ClientLevel level, BedrockTorchPlacement placement) {
        if (placement == null || placement.getSupportPos() == null || placement.getTorchPos() == null) {
            return false;
        }
        BlockState supportState = level.getBlockState(placement.getSupportPos());
        if (!supportState.is(Blocks.SLIME_BLOCK) && !BlockUtils.isReplaceable(supportState)) {
            return false;
        }
        BlockState torchState = level.getBlockState(placement.getTorchPos());
        return BlockUtils.isReplaceable(torchState) || isRedstoneTorch(torchState);
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

    public static BedrockTorchPlacement findTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis, BlockPos... blockedPositions) {
        BedrockTorchPlacement topPlacement = findTopTorchPlacement(level, centerPos, excludedAxis, blockedPositions);
        if (topPlacement != null) {
            return topPlacement;
        }
        return findWallTorchPlacement(level, centerPos, excludedAxis, blockedPositions);
    }

    public static BedrockTorchPlacement findTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis) {
        return findTorchPlacement(level, centerPos, excludedAxis, new BlockPos[0]);
    }

    public static BedrockTorchPlacement findPossibleSlimeTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis, BlockPos... blockedPositions) {
        BedrockTorchPlacement topPlacement = findPossibleTopSlimeTorchPlacement(level, centerPos, excludedAxis, blockedPositions);
        if (topPlacement != null) {
            return topPlacement;
        }
        return findPossibleWallSlimeTorchPlacement(level, centerPos, excludedAxis, blockedPositions);
    }

    private static BedrockTorchPlacement findTopTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis, BlockPos... blockedPositions) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            if (excludedAxis != null && direction == excludedAxis) {
                continue;
            }
            BedrockTorchPlacement placement = new BedrockTorchPlacement(centerPos.relative(direction), Direction.UP);
            if (!conflictsWithBlockedPositions(placement, blockedPositions) && isTorchPlacementUsable(level, placement)) {
                return placement;
            }
        }
        return null;
    }

    private static BedrockTorchPlacement findWallTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis, BlockPos... blockedPositions) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            if (excludedAxis != null && direction == excludedAxis) {
                continue;
            }
            BlockPos torchPos = centerPos.relative(direction);
            for (Direction attachedFace : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
                if (excludedAxis != null && attachedFace == excludedAxis) {
                    continue;
                }
                BedrockTorchPlacement placement = new BedrockTorchPlacement(torchPos.relative(attachedFace.getOpposite()), attachedFace);
                if (!conflictsWithBlockedPositions(placement, blockedPositions) && isTorchPlacementUsable(level, placement)) {
                    return placement;
                }
            }
        }
        return null;
    }

    private static BedrockTorchPlacement findPossibleTopSlimeTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis, BlockPos... blockedPositions) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            if (excludedAxis != null && direction == excludedAxis) {
                continue;
            }
            BedrockTorchPlacement placement = new BedrockTorchPlacement(centerPos.relative(direction), Direction.UP);
            if (!conflictsWithBlockedPositions(placement, blockedPositions) && isSlimePlacementUsable(level, placement)) {
                return placement;
            }
        }
        return null;
    }

    private static BedrockTorchPlacement findPossibleWallSlimeTorchPlacement(ClientLevel level, BlockPos centerPos, Direction excludedAxis, BlockPos... blockedPositions) {
        for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            if (excludedAxis != null && direction == excludedAxis) {
                continue;
            }
            BlockPos torchPos = centerPos.relative(direction);
            for (Direction attachedFace : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
                if (excludedAxis != null && attachedFace == excludedAxis) {
                    continue;
                }
                BedrockTorchPlacement placement = new BedrockTorchPlacement(torchPos.relative(attachedFace.getOpposite()), attachedFace);
                if (!conflictsWithBlockedPositions(placement, blockedPositions) && isSlimePlacementUsable(level, placement)) {
                    return placement;
                }
            }
        }
        return null;
    }

    private static boolean conflictsWithBlockedPositions(BedrockTorchPlacement placement, BlockPos... blockedPositions) {
        if (placement == null || blockedPositions == null) {
            return false;
        }
        BlockPos supportPos = placement.getSupportPos();
        BlockPos torchPos = placement.getTorchPos();
        for (BlockPos blockedPos : blockedPositions) {
            if (blockedPos == null) {
                continue;
            }
            if (blockedPos.equals(supportPos) || blockedPos.equals(torchPos)) {
                return true;
            }
        }
        return false;
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
