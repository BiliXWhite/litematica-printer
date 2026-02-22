package me.aleksilassila.litematica.printer.handler;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GuiBlockInfo {
    public final ClientLevel level;
    public final Identifier world;
    public final @Nullable WorldSchematic schematic;
    public final BlockPos pos;
    public final BlockState currentState;
    public final @Nullable BlockState requiredState;
    public @Nullable SchematicBlockContext context;
    public boolean interacted = false;
    public boolean execute = false;
    public boolean posInSelectionRange = false;

    public GuiBlockInfo(ClientLevel level, @Nullable WorldSchematic schematic, BlockPos pos) {
        this.level = level;
        this.world = level.dimension().identifier();
        this.schematic = schematic;
        this.pos = pos;
        this.currentState = level.getBlockState(pos);
        if (schematic == null) {
            this.requiredState = null;
        } else {
            this.requiredState = schematic.getBlockState(pos.above());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GuiBlockInfo that = (GuiBlockInfo) o;
        return Objects.equals(level, that.level) && Objects.equals(world, that.world) && Objects.equals(pos, that.pos) && Objects.equals(currentState, that.currentState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, world, pos, currentState);
    }

    @Override
    public String toString() {
        return "GuiBlockInfo{" +
                "level=" + level +
                ", world=" + world +
                ", pos=" + pos +
                ", state=" + currentState +
                '}';
    }
}
