package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class BedrockTorchPlacement {
    private final BlockPos supportPos;
    private final Direction clickedFace;

    public BedrockTorchPlacement(BlockPos supportPos, Direction clickedFace) {
        this.supportPos = supportPos;
        this.clickedFace = clickedFace;
    }

    public BlockPos getSupportPos() {
        return this.supportPos;
    }

    public Direction getClickedFace() {
        return this.clickedFace;
    }

    public BlockPos getTorchPos() {
        return this.supportPos == null || this.clickedFace == null
                ? null
                : this.supportPos.relative(this.clickedFace);
    }
}
