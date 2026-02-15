package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PrinterUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();

    public static Direction[] horizontalDirections = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static Direction getHalf(Half half) {
        return half == Half.TOP ? Direction.UP : Direction.DOWN;
    }

    public static Comparable<?> getPropertyByName(BlockState state, String name) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase(name)) {
                return state.getValue(prop);
            }
        }
        return null;
    }

    public static boolean canBeClicked(ClientLevel world, BlockPos pos) {
        return getOutlineShape(world, pos) != Shapes.empty();
    }

    public static VoxelShape getOutlineShape(ClientLevel world, BlockPos pos) {
        return world.getBlockState(pos).getShape(world, pos);
    }

    public static Map<Direction, Vec3> getSlabSides(Level world, BlockPos pos, SlabType requiredHalf) {
        if (requiredHalf == SlabType.DOUBLE) requiredHalf = SlabType.BOTTOM;
        Direction requiredDir = requiredHalf == SlabType.TOP ? Direction.UP : Direction.DOWN;
        Map<Direction, Vec3> sides = new HashMap<>();
        sides.put(requiredDir, new Vec3(0, 0, 0));
        if (world.getBlockState(pos).hasProperty(SlabBlock.TYPE)) {
            sides.put(requiredDir.getOpposite(), Vec3.atLowerCornerOf(DirectionUtils.getVector(requiredDir)).scale(0.5));
        }
        for (Direction side : horizontalDirections) {
            BlockState neighborCurrentState = world.getBlockState(pos.relative(side));
            if (neighborCurrentState.hasProperty(SlabBlock.TYPE) && neighborCurrentState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) {
                if (neighborCurrentState.getValue(SlabBlock.TYPE) != requiredHalf) {
                    continue;
                }
            }
            sides.put(side, Vec3.atLowerCornerOf(DirectionUtils.getVector(requiredDir)).scale(0.25));
        }
        return sides;
    }
}
