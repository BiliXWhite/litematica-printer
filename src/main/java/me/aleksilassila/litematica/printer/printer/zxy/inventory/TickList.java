package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TickList {
    public Block block;
    public ServerWorld world;
    public BlockPos pos;
    public BlockState state;

    public TickList(Block block, ServerWorld world, BlockPos pos, BlockState state) {
        this.block = block;
        this.world = world;
        this.pos = pos;
        this.state = state;
    }
}
