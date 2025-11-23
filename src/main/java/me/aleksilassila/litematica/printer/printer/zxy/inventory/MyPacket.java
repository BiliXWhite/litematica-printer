package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record MyPacket(BlockState blockState, boolean isOpen) {
    // 用于序列化数据以发送给客户端的方法
    public static void encode(MyPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Block.getId(msg.blockState));
        buffer.writeBoolean(msg.isOpen);
    }

    // 用于接收客户端数据的方法
    public static MyPacket decode(FriendlyByteBuf buffer) {
        return new MyPacket(Block.stateById(buffer.readVarInt()), buffer.readBoolean());
    }
}
