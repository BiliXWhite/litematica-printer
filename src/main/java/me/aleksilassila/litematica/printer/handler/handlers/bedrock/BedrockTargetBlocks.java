package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BedrockTargetBlocks {
    private BedrockTargetBlocks() {
    }

    public static boolean isTargetBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) {
            return true;
        }
        //#if MC > 12004
        if (state.is(Blocks.VAULT) || state.is(Blocks.TRIAL_SPAWNER)) {
            return true;
        }
        //#endif
        return false;
    }

    public static boolean isCleanupResidue(BlockState state) {
        return state.is(Blocks.PISTON)
                || state.is(Blocks.MOVING_PISTON)
                || state.is(Blocks.PISTON_HEAD)
                || state.is(Blocks.REDSTONE_TORCH)
                || state.is(Blocks.SLIME_BLOCK);
    }
}
