package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.Items;

public final class BedrockBreaker {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private BedrockBreaker() {
    }

    public static boolean breakBlock(BlockPos pos) {
        return breakBlock(pos, true);
    }

    public static boolean breakBlock(BlockPos pos, boolean predictRemoval) {
        if (CLIENT.level == null || CLIENT.player == null) {
            BedrockDebugLog.write("break skipped pos=" + BedrockDebugLog.pos(pos) + " reason=no_level_or_player");
            return false;
        }
        var state = CLIENT.level.getBlockState(pos);
        if (state.isAir()) {
            BedrockDebugLog.write("break skipped pos=" + BedrockDebugLog.pos(pos) + " reason=air");
            return false;
        }
        if (!BedrockInventory.switchToItem(Items.DIAMOND_PICKAXE) && !BedrockInventory.switchToItem(Items.NETHERITE_PICKAXE)) {
            BedrockDebugLog.write("break skipped pos=" + BedrockDebugLog.pos(pos) + " reason=missing_pickaxe");
            return false;
        }

        BedrockDebugLog.write("break start pos=" + BedrockDebugLog.pos(pos)
                + " state=" + BedrockDebugLog.describeState(state)
                + " predictRemoval=" + predictRemoval);

        //#if MC >= 11900
        NetworkUtils.sendPacket(sequence -> new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                Direction.DOWN,
                sequence
        ));
        NetworkUtils.sendPacket(sequence -> new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                Direction.DOWN,
                sequence
        ));
        //#else
        //$$ NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(
        //$$         ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
        //$$         pos,
        //$$         Direction.DOWN
        //$$ ));
        //$$ NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(
        //$$         ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
        //$$         pos,
        //$$         Direction.DOWN
        //$$ ));
        //#endif

        // For sync-sensitive targets we wait for the server/chunk refresh instead of
        // forcing the client state ahead, which can otherwise stall the state machine.
        if (predictRemoval) {
            CLIENT.level.removeBlock(pos, false);
        }

        return true;
    }
}
