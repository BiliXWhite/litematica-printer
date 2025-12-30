package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

public class InventoryPacket extends FriendlyByteBuf {
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

    public BlockState readBlockState() {
        return blockState;
    }

    public void writeBlockState(BlockState state) {
        blockState = state;
    }
}
