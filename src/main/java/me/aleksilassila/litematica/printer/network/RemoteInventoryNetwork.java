//#if MC >= 12005
package me.aleksilassila.litematica.printer.network;

import lombok.Setter;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import me.aleksilassila.litematica.printer.network.payload.GetItemFromInventoryPayload;
import me.aleksilassila.litematica.printer.network.payload.GetItemResultPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class RemoteInventoryNetwork {
    @Setter
    private static BiConsumer<BlockPos, RemoteResultType> resultCallback;
    @Setter
    private static Consumer<ScanContainerResultPayload> scanResultCallback;

    public static void register() {
        try {
            PayloadTypeRegistry.playC2S().register(GetItemFromInventoryPayload.TYPE, GetItemFromInventoryPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(GetItemResultPayload.TYPE, GetItemResultPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(ScanContainerPayload.TYPE, ScanContainerPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ScanContainerResultPayload.TYPE, ScanContainerResultPayload.CODEC);
        } catch (IllegalArgumentException ignored) {
            Reference.LOGGER.warn("Failed to register remote inventory payloads, maybe already registered?");
        }

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            ClientPlayNetworking.registerReceiver(GetItemResultPayload.TYPE, (payload, context) -> {
                if (resultCallback != null) {
                    context.client().execute(() ->
                            resultCallback.accept(payload.getPos(), payload.getResultType()));
                }
            });
            ClientPlayNetworking.registerReceiver(ScanContainerResultPayload.TYPE, (payload, context) -> {
                if (scanResultCallback != null) {
                    context.client().execute(() -> scanResultCallback.accept(payload));
                }
            });
        });
    }

    public static void sendGetItemRequest(BlockPos containerPos, String itemId, int slot) {
        ClientPlayNetworking.send(new GetItemFromInventoryPayload(itemId, containerPos, slot));
    }

    public static void sendScanContainerRequest(BlockPos containerPos) {
        ClientPlayNetworking.send(new ScanContainerPayload(containerPos));
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network;
//$$
//$$ import io.netty.buffer.Unpooled;
//$$ import me.aleksilassila.litematica.printer.enums.RemoteResultType;
//$$ import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
//$$ import net.fabricmc.api.EnvType;
//$$ import net.fabricmc.api.Environment;
//$$ import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//$$ import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.network.FriendlyByteBuf;
//$$ import net.minecraft.resources.ResourceLocation;
//$$
//$$ import java.util.ArrayList;
//$$ import java.util.List;
//$$ import java.util.function.BiConsumer;
//$$ import java.util.function.Consumer;
//$$
//$$ @Environment(EnvType.CLIENT)
//$$ public class RemoteInventoryNetwork {
//$$     private static BiConsumer<BlockPos, RemoteResultType> resultCallback;
//$$     private static Consumer<ScanContainerResultPayload> scanResultCallback;
//$$
//$$     public static void setResultCallback(BiConsumer<BlockPos, RemoteResultType> callback) {
//$$         resultCallback = callback;
//$$     }
//$$
//$$     public static void setScanResultCallback(Consumer<ScanContainerResultPayload> callback) {
//$$         scanResultCallback = callback;
//$$     }
//$$
//$$     public static void register() {
//$$         ClientPlayConnectionEvents.INIT.register((handler, client) -> {
//$$             ClientPlayNetworking.registerReceiver(
//$$                 new ResourceLocation("remote-inventory-server", "get_item_result"),
//$$                 (client1, handler1, buf, responseSender) -> {
//$$                     BlockPos pos = buf.readBlockPos();
//$$                     RemoteResultType result = RemoteResultType.values()[buf.readVarInt()];
//$$                     if (resultCallback != null) {
//$$                         client1.execute(() -> resultCallback.accept(pos, result));
//$$                     }
//$$                 }
//$$             );
//$$             ClientPlayNetworking.registerReceiver(
//$$                 new ResourceLocation("remote-inventory-server", "scan_container_result"),
//$$                 (client1, handler1, buf, responseSender) -> {
//$$                     BlockPos pos = buf.readBlockPos();
//$$                     int size = buf.readVarInt();
//$$                     List<ScanContainerResultPayload.SlotEntry> entries = new ArrayList<>(size);
//$$                     for (int i = 0; i < size; i++) {
//$$                         int slot = buf.readVarInt();
//$$                         String itemId = buf.readResourceLocation().toString();
//$$                         int count = buf.readVarInt();
//$$                         entries.add(new ScanContainerResultPayload.SlotEntry(slot, itemId, count));
//$$                     }
//$$                     ScanContainerResultPayload payload =
//$$                             new ScanContainerResultPayload(pos, entries);
//$$                     if (scanResultCallback != null) {
//$$                         client1.execute(() -> scanResultCallback.accept(payload));
//$$                     }
//$$                 }
//$$             );
//$$         });
//$$     }
//$$
//$$     public static void sendGetItemRequest(BlockPos containerPos, String itemId, int slot) {
//$$         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//$$         buf.writeResourceLocation(new ResourceLocation(itemId));
//$$         buf.writeBlockPos(containerPos);
//$$         buf.writeVarInt(slot);
//$$         ClientPlayNetworking.send(
//$$             new ResourceLocation("remote-inventory-server", "get_item_from_inventory"),
//$$             buf
//$$         );
//$$     }
//$$
//$$     public static void sendScanContainerRequest(BlockPos containerPos) {
//$$         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//$$         buf.writeBlockPos(containerPos);
//$$         ClientPlayNetworking.send(
//$$             new ResourceLocation("remote-inventory-server", "scan_container"),
//$$             buf
//$$         );
//$$     }
//$$ }
//#endif
