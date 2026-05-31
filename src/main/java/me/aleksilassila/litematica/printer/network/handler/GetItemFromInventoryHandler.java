//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.handler;

import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.container.ContainerItemResolver;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import me.aleksilassila.litematica.printer.network.payload.GetItemFromInventoryPayload;
import me.aleksilassila.litematica.printer.network.payload.GetItemResultPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class GetItemFromInventoryHandler {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
            GetItemFromInventoryPayload.TYPE,
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
                                GetItemFromInventoryPayload payload) {
        server.execute(() -> {
            try {
                RemoteResultType result = ContainerItemResolver.resolveItem(
                    player,
                    payload.getPos(),
                    payload.getItemId(),
                    payload.getSlot()
                );
                sendResult(player, payload.getPos(), result);
            } catch (Exception e) {
                Reference.LOGGER.error(
                    "Error processing inventory request from {}: {}",
                    player.getName().getString(), e.getMessage(), e
                );
                sendResult(player, payload.getPos(), RemoteResultType.INTERNAL_ERROR);
            }
        });
    }

    private static void sendResult(ServerPlayer player, BlockPos pos,
                                    RemoteResultType resultType) {
        ServerPlayNetworking.send(player, new GetItemResultPayload(pos, resultType));
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.handler;
//$$
//$$ import me.aleksilassila.litematica.printer.Reference;
//$$ import me.aleksilassila.litematica.printer.container.ContainerItemResolver;
//$$ import me.aleksilassila.litematica.printer.enums.RemoteResultType;
//$$ import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//$$ import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.network.FriendlyByteBuf;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.server.MinecraftServer;
//$$ import net.minecraft.server.level.ServerPlayer;
//$$
//$$ public class GetItemFromInventoryHandler {
//$$     private static final ResourceLocation REQUEST_ID =
//$$             new ResourceLocation("remote-inventory-server", "get_item_from_inventory");
//$$     private static final ResourceLocation RESPONSE_ID =
//$$             new ResourceLocation("remote-inventory-server", "get_item_result");
//$$
//$$     public static void register() {
//$$         ServerPlayNetworking.registerGlobalReceiver(
//$$             REQUEST_ID,
//$$             (server, player, handler, buf, responseSender) -> {
//$$                 ResourceLocation itemId = buf.readResourceLocation();
//$$                 BlockPos pos = buf.readBlockPos();
//$$                 int slot = buf.readVarInt();
//$$                 server.execute(() -> {
//$$                     try {
//$$                         RemoteResultType result = ContainerItemResolver.resolveItem(
//$$                             player, pos, itemId.toString(), slot
//$$                         );
//$$                         FriendlyByteBuf responseBuf = PacketByteBufs.create();
//$$                         responseBuf.writeBlockPos(pos);
//$$                         responseBuf.writeEnum(result);
//$$                         responseSender.sendPacket(
//$$                             ServerPlayNetworking.createS2CPacket(RESPONSE_ID, responseBuf)
//$$                         );
//$$                     } catch (Exception e) {
//$$                         Reference.LOGGER.error(
//$$                             "Error processing inventory request from {}: {}",
//$$                             player.getName().getString(), e.getMessage(), e
//$$                         );
//$$                     }
//$$                 });
//$$             }
//$$         );
//$$     }
//$$ }
//#endif
