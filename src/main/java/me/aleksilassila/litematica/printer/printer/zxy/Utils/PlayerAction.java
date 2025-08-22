package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;


//#if MC > 12105
//$$ import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
//$$ import net.minecraft.util.PlayerInput;
//#endif

import static me.aleksilassila.litematica.printer.printer.Printer.canBreakBlock;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.canInteracted;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.tick;

public class PlayerAction {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    public static boolean isExistPlayer(){
        return client.player != null;
    }
    public static void interactBlock(Hand hand, Vec3d vec3d, Direction direction, BlockPos pos, boolean insideBlock, boolean useShift){
        if (useShift) setShift(client.player, true);
        client.interactionManager.interactBlock(client.player,
                //#if MC < 11902
                //$$ client.world,
                //#endif
                hand, new BlockHitResult(vec3d, direction, pos, insideBlock));
        if (useShift) setShift(client.player, false);
    }

    public static void setShift(ClientPlayerEntity player , boolean shift){
        //#if MC > 12105
        //$$ PlayerInput input = new PlayerInput(player.input.playerInput.forward(), player.input.playerInput.backward(), player.input.playerInput.left(), player.input.playerInput.right(), player.input.playerInput.jump(), shift, player.input.playerInput.sprint());
        //$$ PlayerInputC2SPacket packet = new PlayerInputC2SPacket(input);
        //#else
        ClientCommandC2SPacket packet = new ClientCommandC2SPacket(player, shift ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY);
        //#endif

        player.networkHandler.sendPacket(packet);

    }

    static BlockPos breakTargetBlock = null;
    static int startTick = -1;
    //如果返回了null则表示正在挖掘该方块
    public static BlockPos excavateBlock(BlockPos pos){
        if (!canInteracted(pos)) {
            breakTargetBlock = null;
            return null;
        }
        //一个游戏刻挖一次就好
        if (startTick == tick) {
            return null;
        }
        breakTargetBlock = breakTargetBlock != null ? breakTargetBlock : pos;
        if (!waJue(breakTargetBlock)) {
            BlockPos breakTargetBlock1 = breakTargetBlock;
            breakTargetBlock = null;
            return breakTargetBlock1;
        }
        startTick = tick;
        return null;
    }

    public static boolean waJue(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
        Block block = currentState.getBlock();
        if (canBreakBlock(pos)) {
            client.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
            client.interactionManager.cancelBlockBreaking();
            return world.getBlockState(pos).isOf(block);
        }
        return false;
    }

}
