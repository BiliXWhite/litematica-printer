package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BedrockTargetBlocks {
    private BedrockTargetBlocks() {
    }

    public static boolean isTargetBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.END_PORTAL)) {
            return true;
        }
        //#if MC > 12004
        if (state.is(Blocks.VAULT) || state.is(Blocks.TRIAL_SPAWNER)) {
            return true;
        }
        //#endif
        for (String rule : Configs.Bedrock.BEDROCK_WHITELIST.getStrings()) {
            if (FilterUtils.matchBlockName(rule, state)) {
                return true;
            }
        }
        return false;
    }

    public static boolean requiresConservativeSync(BlockState state) {
        //#if MC > 12004
        return state.is(Blocks.VAULT) || state.is(Blocks.TRIAL_SPAWNER);
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean isCleanupResidue(BlockState state) {
        return state.is(Blocks.PISTON)
                || state.is(Blocks.MOVING_PISTON)
                || state.is(Blocks.PISTON_HEAD)
                || state.is(Blocks.REDSTONE_TORCH)
                || state.is(Blocks.REDSTONE_WALL_TORCH)
                || state.is(Blocks.SLIME_BLOCK);
    }

    public static boolean requiresSneakPlacement(BlockState state) {
        return Implementation.isInteractive(state.getBlock());
    }
}
