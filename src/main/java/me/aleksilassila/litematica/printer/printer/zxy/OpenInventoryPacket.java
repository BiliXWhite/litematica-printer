package me.aleksilassila.litematica.printer.printer.zxy;

import io.netty.buffer.Unpooled;
import me.aleksilassila.litematica.printer.printer.memory.MemoryUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import red.jackf.chesttracker.memory.MemoryDatabase;

import java.io.*;
import java.util.ArrayList;

import static me.aleksilassila.litematica.printer.printer.Printer.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils.adding;

public class OpenInventoryPacket{
    public static boolean openIng = false;
    public static RegistryKey<World> key = null;
    public static BlockPos pos = null;
    private static final Identifier OPEN_INVENTORY = new Identifier("remoteinventory", "open_inventory");
    private static final Identifier OPEN_RETURN = new Identifier("openreturn", "open_return");
    public static ArrayList<String> playerlist = new ArrayList<>();
    public static void registerReceivePacket(){
        ClientPlayNetworking.registerGlobalReceiver(OPEN_RETURN,(client,playNetworkHandler,packetByteBuf,packetSender)->{
            boolean o = packetByteBuf.readBoolean();
            if(!o){
                client.execute(OpenInventoryPacket::openFail);
            }
        });
//        ServerPlayNetworking.registerGlobalReceiver(OPEN_INVENTORY, (server, player, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
//            server.execute(() -> openInv(server,player, packetByteBuf.readBlockPos(), RegistryKey.of(Registry.WORLD_KEY, packetByteBuf.readIdentifier())));
//        });
    }

    public static void openInv(MinecraftServer server, ServerPlayerEntity player, BlockPos pos, RegistryKey<World> key){

        World world = server.getWorld(key);
        if(world == null) return;
        BlockState blockState = world.getBlockState(pos);
        playerlist.add(player.getEntityName());
        blockState.onUse(world,player, Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        System.out.println("player" + player.getName());
    }
    public static void sendOpenInventory(BlockPos pos, RegistryKey<World> key){
        openIng = true;
        OpenInventoryPacket.pos = pos;
        OpenInventoryPacket.key = key;
//        System.out.println(pos+"   key: "+key);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeIdentifier(key.getValue());
        ClientPlayNetworking.send(OPEN_INVENTORY, new PacketByteBuf(buf));
    }

    public static void openFail(){
        System.out.println("fail");
        MemoryDatabase.getCurrent().removePos(key.getValue() , pos);
        me.aleksilassila.litematica.printer.printer.memory.MemoryDatabase.getCurrent().removePos(key.getValue() , pos);
        MinecraftClient.getInstance().player.closeHandledScreen();
        openIng = false;
        isOpenHandler = false;
        key = null;
        pos = null;
    }
}
