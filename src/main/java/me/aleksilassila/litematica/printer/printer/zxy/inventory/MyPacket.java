package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;

public record MyPacket(BlockState blockState, boolean isOpen) {
    // 用于序列化数据以发送给客户端的方法
    public static void encode(MyPacket msg, PacketByteBuf buffer) {
        buffer.writeVarInt(Block.getRawIdFromState(msg.blockState));
        buffer.writeBoolean(msg.isOpen);
    }

    // 用于接收客户端数据的方法
    public static MyPacket decode(PacketByteBuf buffer) {
        return new MyPacket(Block.getStateFromRawId(buffer.readVarInt()), buffer.readBoolean());
    }
}
