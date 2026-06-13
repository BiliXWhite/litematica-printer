//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.payload;

import io.netty.buffer.ByteBuf;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoteExchangeResultPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoteExchangeResultPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    //#if MC >= 12101
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("remote-inventory-server", "exchange_result")
                    //#else
                    //$$ new net.minecraft.resources.ResourceLocation("remote-inventory-server", "exchange_result")
                    //#endif
            );

    public static final StreamCodec<ByteBuf, RemoteExchangeResultPayload> CODEC =
            StreamCodec.ofMember(RemoteExchangeResultPayload::write, RemoteExchangeResultPayload::decode);

    // exchange 后背包变动 slot 的快照：slotIndex 是 0-35 主背包格，itemId 空则此格为空
    public record SlotSnapshot(int slotIndex, String itemId, int count) {}

    private final BlockPos pos;
    private final RemoteResultType takeResult;
    private final int takenCount;
    private final int returnedCount;
    private final List<SlotSnapshot> inventoryDelta;

    public RemoteExchangeResultPayload(BlockPos pos, RemoteResultType takeResult,
                                       int takenCount, int returnedCount,
                                       List<SlotSnapshot> inventoryDelta) {
        this.pos = pos;
        this.takeResult = takeResult;
        this.takenCount = takenCount;
        this.returnedCount = returnedCount;
        this.inventoryDelta = inventoryDelta != null ? inventoryDelta : Collections.emptyList();
    }

    public BlockPos getPos() { return pos; }
    public RemoteResultType getTakeResult() { return takeResult; }
    public int getTakenCount() { return takenCount; }
    public int getReturnedCount() { return returnedCount; }
    public List<SlotSnapshot> getInventoryDelta() { return inventoryDelta; }

    public static RemoteExchangeResultPayload decode(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        BlockPos pos = wrapped.readBlockPos();
        RemoteResultType takeResult = wrapped.readEnum(RemoteResultType.class);
        int takenCount = wrapped.readVarInt();
        int returnedCount = wrapped.readVarInt();
        int deltaSize = wrapped.readVarInt();
        List<SlotSnapshot> delta = new ArrayList<>(deltaSize);
        for (int i = 0; i < deltaSize; i++) {
            int slotIndex = wrapped.readVarInt();
            String itemId = wrapped.readUtf();
            int count = wrapped.readVarInt();
            delta.add(new SlotSnapshot(slotIndex, itemId, count));
        }
        return new RemoteExchangeResultPayload(pos, takeResult, takenCount, returnedCount, delta);
    }

    public void write(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        wrapped.writeBlockPos(pos);
        wrapped.writeEnum(takeResult);
        wrapped.writeVarInt(takenCount);
        wrapped.writeVarInt(returnedCount);
        wrapped.writeVarInt(inventoryDelta.size());
        for (SlotSnapshot s : inventoryDelta) {
            wrapped.writeVarInt(s.slotIndex());
            wrapped.writeUtf(s.itemId());
            wrapped.writeVarInt(s.count());
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.payload;
//$$
//$$ import me.aleksilassila.litematica.printer.enums.RemoteResultType;
//$$ import net.minecraft.core.BlockPos;
//$$
//$$ public class RemoteExchangeResultPayload {
//$$     private final BlockPos pos;
//$$     private final RemoteResultType takeResult;
//$$     private final int takenCount;
//$$     private final int returnedCount;
//$$
//$$     public RemoteExchangeResultPayload(BlockPos pos, RemoteResultType takeResult, int takenCount, int returnedCount,
//$$                                        java.util.List<SlotSnapshot> inventoryDelta) {
//$$         this.pos = pos; this.takeResult = takeResult; this.takenCount = takenCount; this.returnedCount = returnedCount;
//$$     }
//$$     public record SlotSnapshot(int slotIndex, String itemId, int count) {}
//$$     public BlockPos getPos() { return pos; }
//$$     public RemoteResultType getTakeResult() { return takeResult; }
//$$     public int getTakenCount() { return takenCount; }
//$$     public int getReturnedCount() { return returnedCount; }
//$$     public java.util.List<SlotSnapshot> getInventoryDelta() { return java.util.Collections.emptyList(); }
//$$ }
//#endif
