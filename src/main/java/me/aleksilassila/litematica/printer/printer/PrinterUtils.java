package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.interfaces.Implementation;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PrinterUtils {

    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();

	public static Direction[] horizontalDirections = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public static boolean playerHasAccessToItem(ClientPlayerEntity playerEntity, Item item) {
		return playerHasAccessToItems(playerEntity, new Item[]{item});
	}

	public static boolean playerHasAccessToItems(ClientPlayerEntity playerEntity, Item[] items) {
		if (items == null || items.length == 0) return true;
		if (Implementation.getAbilities(playerEntity).creativeMode) return true;
		else {
            if (!client.player.currentScreenHandler.equals(client.player.playerScreenHandler)) return false;
			Inventory inv = Implementation.getInventory(playerEntity);
			for (Item item : items) {
				for (int i = 0; i < inv.size(); i++) {
					if (inv.getStack(i).getItem() == item && inv.getStack(i).getCount() > 0) {
                        return true;
                    }
				}
                Printer.remoteItem.add(item);
            }
		}
        return false;
	}

    protected static boolean isDoubleSlab(BlockState state) {
    	return state.contains(SlabBlock.TYPE) && state.get(SlabBlock.TYPE) == SlabType.DOUBLE;
    }

	protected static boolean isHalfSlab(BlockState state) {
    	return state.contains(SlabBlock.TYPE) && state.get(SlabBlock.TYPE) != SlabType.DOUBLE;
	}

    public static Direction getHalf(BlockHalf half) {
        return half == BlockHalf.TOP ? Direction.UP : Direction.DOWN;
    }

    public static Direction axisToDirection(Direction.Axis axis) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == axis) return direction;
        }
        return Direction.DOWN;
    }

    public static Comparable<?> getPropertyByName(BlockState state, String name) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase(name)) {
                return state.get(prop);
            }
        }

        return null;
    }

    public static boolean canBeClicked(ClientWorld world, BlockPos pos) {
        return getOutlineShape(world, pos) != VoxelShapes.empty();
    }

    public static VoxelShape getOutlineShape(ClientWorld world, BlockPos pos) {
        return world.getBlockState(pos).getOutlineShape(world, pos);
    }

    public static Map<Direction, Vec3d> getSlabSides(World world, BlockPos pos, SlabType requiredHalf) {
        if (requiredHalf == SlabType.DOUBLE) requiredHalf = SlabType.BOTTOM;
        Direction requiredDir = requiredHalf == SlabType.TOP ? Direction.UP : Direction.DOWN;

        Map<Direction, Vec3d> sides = new HashMap<>();
        sides.put(requiredDir, new Vec3d(0, 0, 0));

        if (world.getBlockState(pos).contains(SlabBlock.TYPE)) {
            sides.put(requiredDir.getOpposite(), Vec3d.of(requiredDir.getVector()).multiply(0.5));
        }

        for (Direction side : horizontalDirections) {
            BlockState neighborCurrentState = world.getBlockState(pos.offset(side));

            if (neighborCurrentState.contains(SlabBlock.TYPE) && neighborCurrentState.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                if (neighborCurrentState.get(SlabBlock.TYPE) != requiredHalf) {
                    continue;
                }
            }

            sides.put(side, Vec3d.of(requiredDir.getVector()).multiply(0.25));
        }

        return sides;
    }
}
