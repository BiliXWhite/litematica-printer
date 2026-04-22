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
            return false;
        }

        // Optimization: Check pickaxe
        var heldItem = CLIENT.player.getMainHandItem().getItem();
        if (heldItem != Items.DIAMOND_PICKAXE && heldItem != Items.NETHERITE_PICKAXE) {
            if (!BedrockInventory.switchToItem(Items.DIAMOND_PICKAXE) && !BedrockInventory.switchToItem(Items.NETHERITE_PICKAXE)) {
                BedrockDebugLog.write("break skipped pos=" + BedrockDebugLog.pos(pos) + " reason=missing_pickaxe");
                return false;
            }
        }

        BedrockDebugLog.write("break start pos=" + BedrockDebugLog.pos(pos) + " predict=" + predictRemoval);

        // Send EXACTLY one pair of packets
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

        if (predictRemoval) {
            CLIENT.level.removeBlock(pos, false);
        }

        return true;
    }
}
