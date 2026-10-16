package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import me.aleksilassila.litematica.printer.interaction.ToolPreparationResult;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BedrockBreaker {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private BedrockBreaker() {
    }

    public static boolean breakBlock(BlockPos pos) {
        return breakBlock(pos, Direction.DOWN, true);
    }

    public static boolean breakBlock(BlockPos pos, boolean predictRemoval) {
        return breakBlock(pos, Direction.DOWN, predictRemoval);
    }

    public static boolean breakBlock(BlockPos pos, Direction direction, boolean predictRemoval) {
        if (CLIENT.level == null || CLIENT.player == null || !BedrockEnvironment.canInteract(pos)) {
            return false;
        }
        var state = CLIENT.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        // MOVING_PISTON has destroySpeed -1: the server's calcBlockBreakingDelta is negative, so it
        // can never reach the 0.7 STOP threshold nor the failedToMine 1.0 auto-break. Attempting it
        // just spams hit sounds forever ("敲击声但没有回收"). Wait for it to settle into a PISTON /
        // PISTON_HEAD / air and let the normal residue path handle it.
        if (state.is(Blocks.MOVING_PISTON)) {
            return false;
        }

        boolean cleanupResidue = BedrockTargetBlocks.isCleanupResidue(state);
        boolean switched = cleanupResidue
                ? BedrockInventory.switchToCleanupTool(state)
                : BedrockInventory.switchToBestTool(state);
        if (!switched) {
            return false;
        }
        if (RuntimeAccess.get().toolSwitchService().prepareForBreak(pos, state, false)
                != ToolPreparationResult.READY) {
            return false;
        }

        if (CLIENT.gameMode instanceof MultiPlayerGameModeExtension gameModeExtension) {
            BlockBreakResult result = gameModeExtension.litematica_printer$continueDestroyBlock(
                    false,
                    pos,
                    direction,
                    false,
                    false
            );
            return result != BlockBreakResult.FAILED && result != BlockBreakResult.ABORTED;
        }
        return false;
    }

    static boolean prepareCriticalTool(BlockPos pistonPos, BlockState pistonState) {
        if (CLIENT.level == null || CLIENT.player == null || pistonPos == null
                || pistonState == null || pistonState.isAir()) {
            return false;
        }
        if (!BedrockInventory.switchToBestTool(pistonState)) {
            return false;
        }
        return RuntimeAccess.get().toolSwitchService().prepareForBreak(pistonPos, pistonState, false)
                == ToolPreparationResult.READY
                && BedrockInventory.canBreakInCriticalBundle(CLIENT.player, pistonState, CLIENT.player.getMainHandItem());
    }

    static void sendCriticalBreakPackets(BlockPos pos, Direction direction) {
        if (CLIENT.level == null || pos == null || direction == null
                || CLIENT.level.getBlockState(pos).isAir()) {
            return;
        }

        //#if MC >= 11900
        NetworkUtils.sendPacket(sequence -> new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                direction,
                sequence
        ));
        NetworkUtils.sendPacket(sequence -> new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                direction,
                sequence
        ));
        //#else
        //$$ NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(
        //$$         ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
        //$$         pos,
        //$$         direction
        //$$ ));
        //$$ NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(
        //$$         ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
        //$$         pos,
        //$$         direction
        //$$ ));
        //#endif
    }
}
