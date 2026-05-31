//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.payload;

import io.netty.buffer.ByteBuf;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class GetItemResultPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GetItemResultPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    //#if MC >= 12101
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("remote-inventory-server", "get_item_result")
                    //#else
                    //$$ new net.minecraft.resources.ResourceLocation("remote-inventory-server", "get_item_result")
                    //#endif
            );

    public static final StreamCodec<ByteBuf, GetItemResultPayload> CODEC = StreamCodec.ofMember(
            GetItemResultPayload::write, GetItemResultPayload::decode
    );

    private final BlockPos pos;
    private final RemoteResultType resultType;

    public GetItemResultPayload(BlockPos pos, RemoteResultType resultType) {
        this.pos = pos;
        this.resultType = resultType;
    }

    public BlockPos getPos() { return pos; }
    public RemoteResultType getResultType() { return resultType; }

    public static GetItemResultPayload decode(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        return new GetItemResultPayload(wrapped.readBlockPos(), wrapped.readEnum(RemoteResultType.class));
    }

    public void write(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        wrapped.writeBlockPos(pos);
        wrapped.writeEnum(resultType);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.payload;
//$$ class GetItemResultPayload {}
//#endif
