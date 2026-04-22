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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BedrockPlacer {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static float lastYaw = 0.0F;
    private static float lastPitch = 0.0F;

    private BedrockPlacer() {
    }

    public static boolean placeSimple(BlockPos supportPos, Direction clickedFace, Item item) {
        LocalPlayer player = CLIENT.player;
        if (player == null) return false;
        if (!BedrockInventory.switchToOffhand(item)) return false;

        rememberLook(player);
        NetworkUtils.sendLookPacket(player, new PlayerLook(clickedFace.getOpposite()));
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), clickedFace, supportPos, false);
        
        // Single packet placement, no local prediction to avoid ghost blocks
        InteractionUtils.INSTANCE.useItemOn(false, InteractionHand.OFF_HAND, hitResult);
        
        restoreLook(player);
        BedrockDebugLog.write("placeSimple support=" + BedrockDebugLog.pos(supportPos) + " item=" + item);
        return true;
    }

    public static boolean placePiston(BlockPos pistonPos, Direction facing) {
        LocalPlayer player = CLIENT.player;
        if (player == null) return false;
        if (!BedrockInventory.switchToOffhand(Blocks.PISTON.asItem())) return false;

        float yaw = player.getYRot();
        float pitch = facing == Direction.DOWN ? -90.0F : 90.0F;
        rememberLook(player);
        NetworkUtils.sendLookPacket(player, yaw, pitch);

        // Click the block BELOW the piston to ensure a solid surface
        BlockPos clickedPos = pistonPos.below();
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(clickedPos), Direction.UP, clickedPos, false);

        // Single packet placement
        InteractionUtils.INSTANCE.useItemOn(false, InteractionHand.OFF_HAND, hitResult);

        restoreLook(player);
        BedrockDebugLog.write("placePiston piston=" + BedrockDebugLog.pos(pistonPos) + " facing=" + facing);
        return true;
    }

    private static void rememberLook(LocalPlayer player) {
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
    }

    private static void restoreLook(LocalPlayer player) {
        NetworkUtils.sendLookPacket(player, lastYaw, lastPitch);
    }
}
