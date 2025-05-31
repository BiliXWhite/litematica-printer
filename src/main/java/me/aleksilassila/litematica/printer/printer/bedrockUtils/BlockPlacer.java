package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BlockPlacer {
    public static void simpleBlockPlacement(TargetBlock tar, BlockPos pos, ItemConvertible item) {

        MinecraftClient minecraftClient = MinecraftClient.getInstance();

        InventoryManager.switchToItem(item);
//        if(item.equals(Blocks.REDSTONE_TORCH) && minecraftClient.world.getBlockState(pos.down()).isAir()){
////            System.out.println(minecraftClient.world.getBlockState(pos.down()));
//            return;
//        }
        tar.temppos.add(pos);
        BlockHitResult hitResult = new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false);
        placeBlockWithoutInteractingBlock(minecraftClient, hitResult);
    }


    private static float yaw;
    private static float pitch;
    private static void resetLook(){
        sendLookPacket(yaw,pitch);
    }
    private static void sendLookPacket(float yaw,float pitch){
        ClientPlayerEntity player = ZxyUtils.client.player;
        if(player == null) return;
        //#if MC > 12101
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(),player.horizontalCollision));
        //#else
        //$$ MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround()));
        //#endif
    }

    public static void pistonPlacement(BlockPos pos, Direction direction) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        double x = pos.getX();

        switch (BreakingFlowController.getWorkingMode()) {
            case CARPET_EXTRA://carpet accurateBlockPlacement支持
                x = x + 2 + direction.getId() * 2;
                break;
            case VANILLA://直接发包，改变服务端玩家实体视角
                PlayerEntity player = minecraftClient.player;
                float pitch;
                switch (direction) {
                    case UP -> pitch = 90f;
                    case DOWN -> pitch = -90f;
                    default -> pitch = 90f;
                }
                yaw = player.getYaw();
                BlockPlacer.pitch = player.getPitch();
                sendLookPacket(player.getYaw(1.0f), pitch);
                break;
        }

        Vec3d vec3d = new Vec3d(x, pos.getY(), pos.getZ());

        InventoryManager.switchToItem(Blocks.PISTON);
        BlockHitResult hitResult = new BlockHitResult(vec3d, Direction.UP, pos, false);
//        minecraftClient.interactionManager.interactBlock(minecraftClient.player, minecraftClient.world, Hand.MAIN_HAND, hitResult);
        placeBlockWithoutInteractingBlock(minecraftClient, hitResult);
        resetLook();
    }

    private static void placeBlockWithoutInteractingBlock(MinecraftClient minecraftClient, BlockHitResult hitResult) {
        ClientPlayerEntity player = minecraftClient.player;
        ItemStack itemStack = player.getStackInHand(Hand.OFF_HAND);

        ZxyUtils.useBlock(Hand.OFF_HAND,hitResult.getPos(),hitResult.getSide(),hitResult.getBlockPos(),hitResult.isInsideBlock());
//        //#if MC < 11904
//        //$$ minecraftClient.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult));
//        //#else
//        minecraftClient.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult,0));
//        //#endif

        if (!itemStack.isEmpty() && !player.getItemCooldownManager().isCoolingDown(
                //#if MC > 12101
                itemStack
                //#else
                //$$ itemStack.getItem()
                //#endif
        )) {
            ItemUsageContext itemUsageContext = new ItemUsageContext(player, Hand.OFF_HAND, hitResult);
            itemStack.useOnBlock(itemUsageContext);

        }
    }
}
