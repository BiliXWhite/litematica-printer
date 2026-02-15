package me.aleksilassila.litematica.printer.handler;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public class GuiBlockInfo {
    public final ClientLevel level;
    public final Identifier world;
    public final BlockPos pos;
    public final BlockState state;
    public boolean interacted = false;
    public boolean execute = false;
    public boolean posInSelectionRange = false;

    public GuiBlockInfo(ClientLevel level, BlockPos pos, BlockState state) {
        this.level = level;
        this.world = level.dimension().identifier();
        this.pos = pos;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GuiBlockInfo that = (GuiBlockInfo) o;
        return Objects.equals(level, that.level) && Objects.equals(world, that.world) && Objects.equals(pos, that.pos) && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, world, pos, state);
    }

    @Override
    public String toString() {
        return "GuiBlockInfo{" +
                "level=" + level +
                ", world=" + world +
                ", pos=" + pos +
                ", state=" + state +
                '}';
    }
}
