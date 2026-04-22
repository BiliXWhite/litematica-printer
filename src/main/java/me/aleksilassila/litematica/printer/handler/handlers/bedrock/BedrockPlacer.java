package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

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
        rememberLook(player);
        NetworkUtils.sendLookPacket(player, new PlayerLook(clickedFace.getOpposite()));
        BlockHitResult hitResult = new BlockHitResult(new Vec3(supportPos.getX(), supportPos.getY(), supportPos.getZ()), clickedFace, supportPos, false);
        placeBlockAggressively(player, hitResult);
        restoreLook(player);
        BedrockDebugLog.write("placeSimple support=" + BedrockDebugLog.pos(supportPos)
                + " face=" + clickedFace
                + " item=" + item
                + " hitPos=" + hitResult.getBlockPos()
                + " playerYaw=" + player.getYRot()
                + " playerPitch=" + player.getXRot());
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
        // Align with the reference branch: piston direction is primarily derived
        // from player pitch, while the hit result targets the placement cell itself.
        float yaw = player.getYRot();
        float pitch = facing == Direction.DOWN ? -90.0F : 90.0F;
        rememberLook(player);
        NetworkUtils.sendLookPacket(player, yaw, pitch);
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3(pistonPos.getX(), pistonPos.getY(), pistonPos.getZ()),
                Direction.UP,
                pistonPos,
                false
        );
        placeBlockAggressively(player, hitResult);
        restoreLook(player);
        BedrockDebugLog.write("placePiston piston=" + BedrockDebugLog.pos(pistonPos)
                + " facing=" + facing
                + " hitFace=" + hitResult.getDirection()
                + " hitPos=" + hitResult.getBlockPos()
                + " sentYaw=" + yaw
                + " sentPitch=" + pitch);
        return true;
    }

    private static void placeBlockAggressively(LocalPlayer player, BlockHitResult hitResult) {
        InteractionUtils.INSTANCE.useItemOn(false, InteractionHand.OFF_HAND, hitResult);
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            offhand.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hitResult));
        }
    }

    private static void rememberLook(LocalPlayer player) {
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
    }

    private static void restoreLook(LocalPlayer player) {
        NetworkUtils.sendLookPacket(player, lastYaw, lastPitch);
    }
}
