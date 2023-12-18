package me.aleksilassila.litematica.printer.printer.zxy;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;

public class InventoryPacket extends PacketByteBuf {
    /**
     * Creates a packet byte buf that delegates its operations to the {@code
     * parent} buf.
     *
     * @param parent the parent, or delegate, buf
     */
    private BlockState blockState = null;
    public InventoryPacket(ByteBuf parent) {
        super(parent);
    }
    public BlockState readBlockState(){
        return blockState;
    }
    public void writeBlockState(BlockState state){
        blockState = state;
    }
}
