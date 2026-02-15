package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.utils.BlockStateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public class SchematicBlockContext {
    public final Minecraft client;
    public final ClientLevel level;
    public final WorldSchematic schematic;
    public final BlockPos blockPos;
    public final BlockState currentState;
    public final BlockState requiredState;

    public SchematicBlockContext(Minecraft client, ClientLevel level, WorldSchematic schematic, BlockPos blockPos) {
        this.client = client;
        this.level = level;
        this.schematic = schematic;
        this.blockPos = blockPos;
        this.currentState = level.getBlockState(blockPos);
        this.requiredState = schematic.getBlockState(blockPos);
    }

    public static <T extends Comparable<T>> Optional<T> getProperty(BlockState blockState, Property<T> property) {
        return BlockStateUtils.getProperty(blockState, property);
    }

    public SchematicBlockContext offset(Direction direction) {
        return new SchematicBlockContext(client, level, schematic, blockPos.relative(direction));
    }

    public <T extends Comparable<T>> Optional<T> getRequiredStateProperty(Property<T> property) {
        return getProperty(requiredState, property);
    }

    public <T extends Comparable<T>> Optional<T> getCurrentStateProperty(Property<T> property) {
        return getProperty(currentState, property);
    }

    public Block getRequiredBlock() {
        return requiredState.getBlock();
    }

    public Block getCurrentBlock() {
        return currentState.getBlock();
    }

    public MutableComponent getRequiredBlockName() {
        return requiredState.getBlock().getName();
    }

    public MutableComponent getCurrentBlockName() {
        return currentState.getBlock().getName();
    }

    @Override
    public String toString() {
        return "SchematicBlockContext{" +
                "blockPos=" + blockPos +
                ", client=" + client +
                ", level=" + level +
                ", schematic=" + schematic +
                ", currentState=" + currentState +
                ", requiredState=" + requiredState +
                '}';
    }
}
