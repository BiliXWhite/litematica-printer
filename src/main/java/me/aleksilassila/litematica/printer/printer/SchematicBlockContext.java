package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import lombok.ToString;
import me.aleksilassila.litematica.printer.utils.minecraft.BlockStateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

@ToString
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

    public static <T extends Comparable<T>> Optional<T> property(BlockState blockState, Property<T> property) {
        return BlockStateUtils.getProperty(blockState, property);
    }

    public SchematicBlockContext offset(Direction direction) {
        return new SchematicBlockContext(client, level, schematic, blockPos.relative(direction));
    }

    public <T extends Comparable<T>> Optional<T> requiredProperty(Property<T> property) {
        return property(requiredState, property);
    }

    public <T extends Comparable<T>> Optional<T> currentProperty(Property<T> property) {
        return property(currentState, property);
    }

    public Block requiredState() {
        return requiredState.getBlock();
    }

    public Block currentState() {
        return currentState.getBlock();
    }

    public MutableComponent requiredBlockName() {
        return requiredState.getBlock().getName();
    }

    public MutableComponent currentBlockName() {
        return currentState.getBlock().getName();
    }
}
