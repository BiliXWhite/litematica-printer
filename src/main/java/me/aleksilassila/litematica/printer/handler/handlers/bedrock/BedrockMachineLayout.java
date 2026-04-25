package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

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
            if (BedrockEnvironment.findTorchSupport(level, layout.getPistonPos()) != null
                    || BedrockEnvironment.findPossibleSlimeSupport(level, layout.getPistonPos()) != null) {
                return layout;
            }
        }
        return null;
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
