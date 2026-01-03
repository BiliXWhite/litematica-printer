package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.utils.BlockStateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public class BlockContext {
    public final Minecraft client;
    public final ClientLevel level;
    public final WorldSchematic schematic;
    public final BlockPos blockPos;
    public final BlockState currentState;
    public final BlockState requiredState;

    public BlockContext(Minecraft client, ClientLevel level, WorldSchematic schematic, BlockPos blockPos) {
        this.client = client;
        this.level = level;
        this.schematic = schematic;
        this.blockPos = blockPos;
        this.currentState = level.getBlockState(blockPos);
        this.requiredState = schematic.getBlockState(blockPos);
    }

    public BlockContext offset(Direction direction) {
        return new BlockContext(client, level, schematic, blockPos.relative(direction));
    }

    public static <T extends Comparable<T>> Optional<T> getProperty(BlockState blockState, Property<T> property) {
        return BlockStateUtils.getProperty(blockState, property);
    }

    public <T extends Comparable<T>> Optional<T> getRequiredStateProperty(Property<T> property) {
        return getProperty(requiredState, property);
    }

    public <T extends Comparable<T>> Optional<T> getCurrentStateProperty(Property<T> property) {
        return getProperty(currentState, property);
    }

    @Override
    public String toString() {
        return "BlockContext{" +
                "client=" + client +
                ", level=" + level +
                ", schematic=" + schematic +
                ", blockPos=" + blockPos +
                ", currentState=" + currentState +
                ", requiredState=" + requiredState +
                '}';
    }
}
