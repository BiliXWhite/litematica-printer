package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Test {
    public static void main(String[] args) {

    }
    public static void t1(){
        MinecraftClient client = ZxyUtils.client;
        ClientPlayerEntity player = client.player;
        client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(1.0f,0.0f,player.isOnGround()
                //#if MC > 12101
                ,player.horizontalCollision
                //#endif
        ));
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        Vec3d offset = client.crosshairTarget.getPos();
        BlockPos blockPos = new BlockPos((int) offset.x, (int) offset.y, (int) offset.z).up();
        ZxyUtils.interactBlock1(Hand.MAIN_HAND,new Vec3d(0.5,0.5,0.5),Direction.UP,blockPos,false);
    }
}