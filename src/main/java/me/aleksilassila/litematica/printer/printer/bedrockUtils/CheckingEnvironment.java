package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class CheckingEnvironment {

    public static BlockPos findNearbyFlatBlockToPlaceRedstoneTorch(ClientWorld world, BlockPos blockPos) {

        if ((sideCoversSmallSquare(world, blockPos.east(), Direction.UP) && (world.getBlockState(blockPos.east().up()).isReplaceable()) || world.getBlockState(blockPos.east().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.east()).isOf(Blocks.AIR))) {
            return blockPos.east();
        } else if ((sideCoversSmallSquare(world, blockPos.west(), Direction.UP) && (world.getBlockState(blockPos.west().up()).isReplaceable()) || world.getBlockState(blockPos.west().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.west()).isOf(Blocks.AIR))) {
            return blockPos.west();
        } else if ((sideCoversSmallSquare(world, blockPos.north(), Direction.UP) && (world.getBlockState(blockPos.north().up()).isReplaceable())  || world.getBlockState(blockPos.north().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.north()).isOf(Blocks.AIR))) {
            return blockPos.north();
        } else if ((sideCoversSmallSquare(world, blockPos.south(), Direction.UP) && (world.getBlockState(blockPos.south().up()).isReplaceable())  || world.getBlockState(blockPos.south().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.south()).isOf(Blocks.AIR))) {
            return blockPos.south();
        }
        return null;
    }

    public static BlockPos findPossibleSlimeBlockPos(ClientWorld world, BlockPos blockPos) {
        if (world.getBlockState(blockPos.east()).isReplaceable() && (world.getBlockState(blockPos.east().up()).isReplaceable())) {
            return blockPos.east();
        } else if (world.getBlockState(blockPos.west()).isReplaceable() && (world.getBlockState(blockPos.west().up()).isReplaceable())) {
            return blockPos.west();
        } else if (world.getBlockState(blockPos.south()).isReplaceable() && (world.getBlockState(blockPos.south().up()).isReplaceable())) {
            return blockPos.south();
        } else if (world.getBlockState(blockPos.north()).isReplaceable() && (world.getBlockState(blockPos.north().up()).isReplaceable())) {
            return blockPos.north();
        }
        return null;
    }

    public static boolean has2BlocksOfPlaceToPlacePiston(ClientWorld world, BlockPos blockPos) {
        if (world.getBlockState(blockPos.up()).getHardness(world, blockPos.up()) == 0) {
            BlockBreaker.breakBlock(world, blockPos.up());
        }
        return world.getBlockState(blockPos.up()).isReplaceable() && world.getBlockState(blockPos.up().up()).isReplaceable();
    }

    public static ArrayList<BlockPos> findNearbyRedstoneTorch(ClientWorld world, BlockPos pistonBlockPos) {
        ArrayList<BlockPos> list = new ArrayList<>();
        if (world.getBlockState(pistonBlockPos.east()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.east());
        }
        if (world.getBlockState(pistonBlockPos.west()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.west());
        }
        if (world.getBlockState(pistonBlockPos.south()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.south());
        }
        if (world.getBlockState(pistonBlockPos.north()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.north());
        }

        pistonBlockPos = pistonBlockPos.up();
        if (world.getBlockState(pistonBlockPos.east()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.east());
        }
        if (world.getBlockState(pistonBlockPos.west()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.west());
        }
        if (world.getBlockState(pistonBlockPos.south()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.south());
        }
        if (world.getBlockState(pistonBlockPos.north()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.north());
        }

        pistonBlockPos = pistonBlockPos.down(2);
        if (world.getBlockState(pistonBlockPos.east()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.east());
        }
        if (world.getBlockState(pistonBlockPos.west()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.west());
        }
        if (world.getBlockState(pistonBlockPos.south()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.south());
        }
        if (world.getBlockState(pistonBlockPos.north()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.north());
        }
        return list;
    }
}
