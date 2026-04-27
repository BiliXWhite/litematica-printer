package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.minecraft.BlockUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.LinkedHashSet;
import java.util.Set;

public final class BedrockMachineLayout {
    private static final Direction[] SEARCH_ORDER = {
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.DOWN
    };

    private final BlockPos bedrockPos;
    private final Direction pistonOffset;
    private final BlockPos pistonPos;
    private final BlockPos headPos;

    private BedrockMachineLayout(BlockPos bedrockPos, Direction pistonOffset) {
        this.bedrockPos = bedrockPos;
        this.pistonOffset = pistonOffset;
        this.pistonPos = bedrockPos.relative(pistonOffset);
        this.headPos = this.pistonPos.relative(pistonOffset);
    }

    public static BedrockMachineLayout find(ClientLevel level, BlockPos bedrockPos) {
        if (level == null || bedrockPos == null) {
            return null;
        }
        for (Direction direction : SEARCH_ORDER) {
            BedrockMachineLayout layout = new BedrockMachineLayout(bedrockPos, direction);
            if (!BedrockEnvironment.hasRoomForPiston(level, layout.getPistonPos(), layout.getPistonOffset())) {
                continue;
            }
            if (BedrockEnvironment.findTorchPlacement(level, layout.getPistonPos(), layout.getPistonOffset().getOpposite(), bedrockPos, layout.getPistonPos(), layout.getHeadPos()) != null
                    || BedrockEnvironment.findPossibleSlimeTorchPlacement(level, layout.getPistonPos(), layout.getPistonOffset().getOpposite(), bedrockPos, layout.getPistonPos(), layout.getHeadPos()) != null) {
                return layout;
            }
        }
        return null;
    }

    public static Set<BlockPos> findCleanupBlockers(ClientLevel level, BlockPos bedrockPos) {
        if (level == null || bedrockPos == null || find(level, bedrockPos) != null) {
            return null;
        }

        LinkedHashSet<BlockPos> bestBlockers = null;
        for (Direction direction : SEARCH_ORDER) {
            BedrockMachineLayout layout = new BedrockMachineLayout(bedrockPos, direction);
            LinkedHashSet<BlockPos> blockers = getCleanupBlockers(level, bedrockPos, layout);
            if (blockers != null && !blockers.isEmpty()
                    && (bestBlockers == null || blockers.size() < bestBlockers.size())) {
                bestBlockers = blockers;
            }
        }

        return bestBlockers;
    }

    private static LinkedHashSet<BlockPos> getCleanupBlockers(ClientLevel level, BlockPos bedrockPos, BedrockMachineLayout layout) {
        LinkedHashSet<BlockPos> blockers = getPistonCleanupBlockers(level, layout.getPistonPos(), layout.getHeadPos());
        if (blockers == null) {
            return null;
        }

        LinkedHashSet<BlockPos> torchBlockers = BedrockEnvironment.findCleanupAwareTorchPlacementBlockers(
                level,
                layout.getPistonPos(),
                layout.getPistonOffset().getOpposite(),
                bedrockPos,
                layout.getPistonPos(),
                layout.getHeadPos()
        );
        if (torchBlockers != null) {
            blockers.addAll(torchBlockers);
            return blockers;
        }

        LinkedHashSet<BlockPos> slimeBlockers = BedrockEnvironment.findCleanupAwareSlimeTorchPlacementBlockers(
                level,
                layout.getPistonPos(),
                layout.getPistonOffset().getOpposite(),
                bedrockPos,
                layout.getPistonPos(),
                layout.getHeadPos()
        );
        if (slimeBlockers != null) {
            blockers.addAll(slimeBlockers);
            return blockers;
        }

        return null;
    }

    private static LinkedHashSet<BlockPos> getPistonCleanupBlockers(ClientLevel level, BlockPos pistonPos, BlockPos headPos) {
        LinkedHashSet<BlockPos> blockers = new LinkedHashSet<>();
        if (!isReplaceableOrCleanupResidue(level, pistonPos, blockers)) {
            return null;
        }
        if (!isReplaceableOrCleanupResidue(level, headPos, blockers)) {
            return null;
        }
        return blockers;
    }

    private static boolean isReplaceableOrCleanupResidue(ClientLevel level, BlockPos pos, LinkedHashSet<BlockPos> blockers) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        var state = level.getBlockState(pos);
        if (BlockUtils.isReplaceable(state)) {
            return true;
        }
        if (BedrockTargetBlocks.isCleanupResidue(state)) {
            blockers.add(pos);
            return true;
        }
        return false;
    }

    public BlockPos getBedrockPos() {
        return this.bedrockPos;
    }

    public Direction getPistonOffset() {
        return this.pistonOffset;
    }

    public BlockPos getPistonPos() {
        return this.pistonPos;
    }

    public BlockPos getHeadPos() {
        return this.headPos;
    }

    public Direction getPistonPlacementFace() {
        return this.pistonOffset;
    }

    public Direction getPrimingFacing() {
        return this.pistonOffset;
    }

    public Direction getExecuteFacing() {
        return this.pistonOffset.getOpposite();
    }
}
