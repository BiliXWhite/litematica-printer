package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.PlayerLook;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BedrockPlacer {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static float lastYaw;
    private static float lastPitch;

    private BedrockPlacer() {
    }

    public static boolean placeSimple(BlockPos supportPos, Direction clickedFace, Item item) {
        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.gameMode == null) {
            BedrockDebugLog.write("placeSimple skipped support=" + BedrockDebugLog.pos(supportPos) + " item=" + item + " reason=no_player_or_gamemode");
            return false;
        }
        if (!BedrockInventory.switchToOffhand(item)) {
            BedrockDebugLog.write("placeSimple skipped support=" + BedrockDebugLog.pos(supportPos) + " item=" + item + " reason=missing_item");
            return false;
        }
        PlayerLook look = new PlayerLook(clickedFace.getOpposite());
        rememberLook(player);
        NetworkUtils.sendLookPacket(player, look);
        syncLocalLook(player, look.getYaw(), look.getPitch());
        // Use center of the support block for more reliable interaction
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), clickedFace, supportPos, false);
        boolean placed = placeBlockAggressively(player, hitResult);
        restoreLook(player);
        if (!placed) {
            BedrockDebugLog.write("placeSimple failed support=" + BedrockDebugLog.pos(supportPos)
                    + " face=" + clickedFace
                    + " item=" + item
                    + " reason=interaction_rejected");
            return false;
        }
        BedrockDebugLog.write("placeSimple support=" + BedrockDebugLog.pos(supportPos)
                + " face=" + clickedFace
                + " item=" + item
                + " hitPos=" + hitResult.getBlockPos());
        return true;
    }

    public static boolean placePiston(BlockPos pistonPos, Direction facing) {
        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.gameMode == null) {
            BedrockDebugLog.write("placePiston skipped piston=" + BedrockDebugLog.pos(pistonPos) + " facing=" + facing + " reason=no_player_or_gamemode");
            return false;
        }
        if (!BedrockInventory.switchToOffhand(Blocks.PISTON.asItem())) {
            BedrockDebugLog.write("placePiston skipped piston=" + BedrockDebugLog.pos(pistonPos) + " facing=" + facing + " reason=missing_piston");
            return false;
        }

        // Pistons face opposite to the direction the player is looking when placed.
        // We want the resulting piston facing to match `facing`, so look at the opposite side.
        PlayerLook look = new PlayerLook(facing.getOpposite());
        rememberLook(player);
        NetworkUtils.sendLookPacket(player, look);
        syncLocalLook(player, look.getYaw(), look.getPitch());

        BlockPos clickedPos = pistonPos.relative(facing.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(clickedPos),
                facing,
                clickedPos,
                false
        );

        boolean placed = placeBlockAggressively(player, hitResult);
        restoreLook(player);
        if (!placed) {
            BedrockDebugLog.write("placePiston failed piston=" + BedrockDebugLog.pos(pistonPos)
                    + " facing=" + facing
                    + " clickedBlock=" + BedrockDebugLog.pos(clickedPos)
                    + " reason=interaction_rejected");
            return false;
        }
        BedrockDebugLog.write("placePiston piston=" + BedrockDebugLog.pos(pistonPos)
                + " facing=" + facing
                + " clickedBlock=" + BedrockDebugLog.pos(clickedPos)
                + " sentYaw=" + look.getYaw()
                + " sentPitch=" + look.getPitch());
        return true;
    }

    private static boolean placeBlockAggressively(LocalPlayer player, BlockHitResult hitResult) {
        boolean useShift = CLIENT.level != null && BedrockTargetBlocks.requiresSneakPlacement(CLIENT.level.getBlockState(hitResult.getBlockPos()));
        boolean wasSneak = player.isShiftKeyDown();
        if (useShift && !wasSneak) {
            ActionManager.INSTANCE.setShift(player, true);
        }
        try {
            InteractionResult result = InteractionUtils.INSTANCE.useItemOn(false, InteractionHand.OFF_HAND, hitResult);
            return result.consumesAction() || result == InteractionResult.SUCCESS;
        } finally {
            if (useShift && !wasSneak) {
                ActionManager.INSTANCE.setShift(player, false);
            }
        }
    }

    private static void rememberLook(LocalPlayer player) {
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
    }

    private static void syncLocalLook(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
    }

    private static void restoreLook(LocalPlayer player) {
        syncLocalLook(player, lastYaw, lastPitch);
        NetworkUtils.sendLookPacket(player, lastYaw, lastPitch);
    }
}
