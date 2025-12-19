package me.aleksilassila.litematica.printer;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SchematicBlockState {
    public final Level world;
    public final WorldSchematic schematic;
    public final BlockPos blockPos;
    public final BlockState targetState;
    public final BlockState currentState;

    public SchematicBlockState(Level world, WorldSchematic schematic, BlockPos blockPos) {
        this.world = world;
        this.schematic = schematic;
        this.blockPos = blockPos;
        this.targetState = schematic.getBlockState(blockPos);
        this.currentState = world.getBlockState(blockPos);
    }

    public SchematicBlockState offset(Direction direction) {
        return new SchematicBlockState(world, schematic, blockPos.relative(direction));
    }

    @Override
    public String toString() {
        return "SchematicBlockState{" +
                "world=" + world +
                ", schematic=" + schematic +
                ", blockPos=" + blockPos +
                ", targetState=" + targetState +
                ", currentState=" + currentState +
                '}';
    }
}