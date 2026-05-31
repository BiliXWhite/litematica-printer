//#if MC >= 12005
package me.aleksilassila.litematica.printer.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ScanContainerPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScanContainerPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    //#if MC >= 12101
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("remote-inventory-server", "scan_container")
                    //#else
                    //$$ new net.minecraft.resources.ResourceLocation("remote-inventory-server", "scan_container")
                    //#endif
            );

    public static final StreamCodec<ByteBuf, ScanContainerPayload> CODEC = StreamCodec.ofMember(
            ScanContainerPayload::write, ScanContainerPayload::decode
    );

    private final BlockPos pos;

    public ScanContainerPayload(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() { return pos; }

    public static ScanContainerPayload decode(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        return new ScanContainerPayload(wrapped.readBlockPos());
    }

    public void write(ByteBuf buf) {
        FriendlyByteBuf wrapped = (FriendlyByteBuf) buf;
        wrapped.writeBlockPos(pos);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#else
//$$ package me.aleksilassila.litematica.printer.network.payload;
//$$ class ScanContainerPayload {}
//#endif
