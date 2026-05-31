//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class GetItemFromInventoryPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GetItemFromInventoryPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    //#if MC >= 12101
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("remote-inventory-server", "get_item_from_inventory")
                    //#else
                    //$$ new net.minecraft.resources.ResourceLocation("remote-inventory-server", "get_item_from_inventory")
                    //#endif
            );

    public static final StreamCodec<ByteBuf, GetItemFromInventoryPayload> CODEC = StreamCodec.ofMember(
            GetItemFromInventoryPayload::write, GetItemFromInventoryPayload::decode
    );

    private final String itemId;
    private final BlockPos pos;
    private final int slot;

    public GetItemFromInventoryPayload(String itemId, BlockPos pos, int slot) {
        this.itemId = itemId;
        this.pos = pos;
        this.slot = slot;
    }

    public String getItemId() { return itemId; }
    public BlockPos getPos() { return pos; }
    public int getSlot() { return slot; }

    public static GetItemFromInventoryPayload decode(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        //#if MC >= 12105
        String itemId = wrapped.readIdentifier().toString();
        //#else
        //$$ String itemId = wrapped.readResourceLocation().toString();
        //#endif
        return new GetItemFromInventoryPayload(itemId, wrapped.readBlockPos(), wrapped.readVarInt());
    }

    public void write(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        //#if MC >= 12105
        wrapped.writeIdentifier(net.minecraft.resources.Identifier.parse(itemId));
        //#elseif MC >= 12101
        //$$ wrapped.writeResourceLocation(net.minecraft.resources.ResourceLocation.parse(itemId));
        //#else
        //$$ wrapped.writeResourceLocation(new net.minecraft.resources.ResourceLocation(itemId));
        //#endif
        wrapped.writeBlockPos(pos);
        wrapped.writeVarInt(slot);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.payload;
//$$ class GetItemFromInventoryPayload {}
//#endif
