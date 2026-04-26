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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BedrockPlacer {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static float lastYaw;
    private static float lastPitch;
    private static Item pendingOffhandItem;
    private static long offhandReadyTick = Long.MIN_VALUE;

    private BedrockPlacer() {
    }

    public static boolean placeSimple(BlockPos supportPos, Direction clickedFace, Item item) {
        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.gameMode == null) {
            BedrockDebugLog.write("placeSimple skipped support=" + BedrockDebugLog.pos(supportPos) + " item=" + item + " reason=no_player_or_gamemode");
            return false;
        }
        if (!ensureOffhandItem(item)) {
            BedrockDebugLog.write("placeSimple skipped support=" + BedrockDebugLog.pos(supportPos) + " item=" + item + " reason=offhand_not_ready");
            return false;
        }
        PlayerLook look = new PlayerLook(clickedFace.getOpposite());
        rememberLook(player);
        NetworkUtils.sendLookPacket(player, look);
        syncLocalLook(player, look.getYaw(), look.getPitch());
        // Use center of the support block for more reliable interaction
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), clickedFace, supportPos, false);
        placeBlockAggressively(player, hitResult);
        restoreLook(player);
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
        if (!ensureOffhandItem(Blocks.PISTON.asItem())) {
            BedrockDebugLog.write("placePiston skipped piston=" + BedrockDebugLog.pos(pistonPos) + " facing=" + facing + " reason=offhand_not_ready");
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

        placeBlockAggressively(player, hitResult);
        restoreLook(player);
        BedrockDebugLog.write("placePiston piston=" + BedrockDebugLog.pos(pistonPos)
                + " facing=" + facing
                + " clickedBlock=" + BedrockDebugLog.pos(clickedPos)
                + " sentYaw=" + look.getYaw()
                + " sentPitch=" + look.getPitch());
        return true;
    }

    private static void placeBlockAggressively(LocalPlayer player, BlockHitResult hitResult) {
        boolean useShift = CLIENT.level != null && BedrockTargetBlocks.requiresSneakPlacement(CLIENT.level.getBlockState(hitResult.getBlockPos()));
        boolean wasSneak = player.isShiftKeyDown();
        if (useShift && !wasSneak) {
            ActionManager.INSTANCE.setShift(player, true);
        }
        try {
            InteractionUtils.INSTANCE.useItemOn(false, InteractionHand.OFF_HAND, hitResult);
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty()) {
                offhand.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hitResult));
            }
        } finally {
            if (useShift && !wasSneak) {
                ActionManager.INSTANCE.setShift(player, false);
            }
        }
    }

    private static boolean ensureOffhandItem(Item item) {
        LocalPlayer player = CLIENT.player;
        if (player == null) {
            return false;
        }
        long currentTick = CLIENT.level != null ? CLIENT.level.getGameTime() : 0L;
        boolean multiplayer = CLIENT.getConnection() != null && CLIENT.getSingleplayerServer() == null;

        if (multiplayer && pendingOffhandItem == item && currentTick < offhandReadyTick) {
            BedrockDebugLog.write("offhand wait item=" + item
                    + " now=" + currentTick
                    + " readyTick=" + offhandReadyTick);
            return false;
        }

        if (player.getOffhandItem().getItem() == item) {
            pendingOffhandItem = item;
            if (!multiplayer || currentTick >= offhandReadyTick) {
                offhandReadyTick = currentTick;
            }
            return true;
        }

        if (!BedrockInventory.switchToOffhand(item)) {
            return false;
        }

        pendingOffhandItem = item;
        if (multiplayer) {
            offhandReadyTick = currentTick + 1L;
            BedrockDebugLog.write("offhand switch queued item=" + item
                    + " now=" + currentTick
                    + " readyTick=" + offhandReadyTick);
            return false;
        }

        offhandReadyTick = currentTick;
        return true;
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
