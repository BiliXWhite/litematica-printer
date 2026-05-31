//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.handler;

import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.container.ContainerItemResolver;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ScanContainerHandler {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
            ScanContainerPayload.TYPE,
            (payload, context) -> {
                //#if MC >= 12100
                MinecraftServer server = context.server();
                //#else
                //$$ MinecraftServer server = context.player().getServer();
                //#endif
                handle(server, context.player(), payload);
            }
        );
    }

    private static void handle(MinecraftServer server, ServerPlayer player,
                                ScanContainerPayload payload) {
        server.execute(() -> {
            try {
                List<ScanContainerResultPayload.SlotEntry> entries =
                        ContainerItemResolver.scanContainer(player, payload.getPos());
                ServerPlayNetworking.send(player,
                        new ScanContainerResultPayload(payload.getPos(), entries));
            } catch (Exception e) {
                Reference.LOGGER.error(
                    "Error scanning container at {} for {}: {}",
                    payload.getPos(), player.getName().getString(), e.getMessage(), e
                );
                ServerPlayNetworking.send(player,
                        new ScanContainerResultPayload(payload.getPos(), List.of()));
            }
        });
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.handler;
//$$
//$$ import me.aleksilassila.litematica.printer.Reference;
//$$ import me.aleksilassila.litematica.printer.container.ContainerItemResolver;
//$$ import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
//$$ import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//$$ import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.network.FriendlyByteBuf;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.server.MinecraftServer;
//$$ import net.minecraft.server.level.ServerPlayer;
//$$
//$$ import java.util.List;
//$$
//$$ public class ScanContainerHandler {
//$$     private static final ResourceLocation REQUEST_ID =
//$$             new ResourceLocation("remote-inventory-server", "scan_container");
//$$     private static final ResourceLocation RESPONSE_ID =
//$$             new ResourceLocation("remote-inventory-server", "scan_container_result");
//$$
//$$     public static void register() {
//$$         ServerPlayNetworking.registerGlobalReceiver(
//$$             REQUEST_ID,
//$$             (server, player, handler, buf, responseSender) -> {
//$$                 BlockPos pos = buf.readBlockPos();
//$$                 server.execute(() -> {
//$$                     try {
//$$                         List<ScanContainerResultPayload.SlotEntry> entries =
//$$                                 ContainerItemResolver.scanContainer(player, pos);
//$$                         FriendlyByteBuf responseBuf = PacketByteBufs.create();
//$$                         responseBuf.writeBlockPos(pos);
//$$                         responseBuf.writeVarInt(entries.size());
//$$                         for (ScanContainerResultPayload.SlotEntry e : entries) {
//$$                             responseBuf.writeVarInt(e.slot());
//$$                             responseBuf.writeResourceLocation(new ResourceLocation(e.itemId()));
//$$                             responseBuf.writeVarInt(e.count());
//$$                         }
//$$                         responseSender.sendPacket(
//$$                             ServerPlayNetworking.createS2CPacket(RESPONSE_ID, responseBuf)
//$$                         );
//$$                     } catch (Exception ex) {
//$$                         Reference.LOGGER.error(
//$$                             "Error scanning container at {} for {}: {}",
//$$                             pos, player.getName().getString(), ex.getMessage(), ex
//$$                         );
//$$                         FriendlyByteBuf emptyBuf = PacketByteBufs.create();
//$$                         emptyBuf.writeBlockPos(pos);
//$$                         emptyBuf.writeVarInt(0);
//$$                         responseSender.sendPacket(
//$$                             ServerPlayNetworking.createS2CPacket(RESPONSE_ID, emptyBuf)
//$$                         );
//$$                     }
//$$                 });
//$$             }
//$$         );
//$$     }
//$$ }
//#endif
