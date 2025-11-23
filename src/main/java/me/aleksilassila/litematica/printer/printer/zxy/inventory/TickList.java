package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TickList {
    public Block block;
    public ServerLevel world;
    public BlockPos pos;
    public BlockState state;

    public TickList(Block block, ServerLevel world, BlockPos pos, BlockState state) {
        this.block = block;
        this.world = world;
        this.pos = pos;
        this.state = state;
    }
}
