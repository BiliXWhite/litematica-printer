package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;

import static me.aleksilassila.litematica.printer.printer.PlacementGuide.Action.isReplaceable;
import static net.minecraft.block.Block.sideCoversSmallSquare;

public class CheckingEnvironment {

    public static BlockPos findNearbyFlatBlockToPlaceRedstoneTorch(ClientWorld world, BlockPos blockPos) {

        if ((sideCoversSmallSquare(world, blockPos.east(), Direction.UP) && (isReplaceable(world.getBlockState(blockPos.east().up()))) || world.getBlockState(blockPos.east().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.east()).isAir())) {
            return blockPos.east();
        } else if ((sideCoversSmallSquare(world, blockPos.west(), Direction.UP) && (isReplaceable(world.getBlockState(blockPos.west().up()))) || world.getBlockState(blockPos.west().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.west()).isAir())) {
            return blockPos.west();
        } else if ((sideCoversSmallSquare(world, blockPos.north(), Direction.UP) && (isReplaceable(world.getBlockState(blockPos.north().up())))  || world.getBlockState(blockPos.north().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.north()).isAir())) {
            return blockPos.north();
        } else if ((sideCoversSmallSquare(world, blockPos.south(), Direction.UP) && (isReplaceable(world.getBlockState(blockPos.south().up())))  || world.getBlockState(blockPos.south().up()).isOf(Blocks.REDSTONE_TORCH) && !world.getBlockState(blockPos.south()).isAir())) {
            return blockPos.south();
        }
        return null;
    }

    public static BlockPos findPossibleSlimeBlockPos(ClientWorld world, BlockPos blockPos) {
        if (isReplaceable(world.getBlockState(blockPos.east().up())) && (isReplaceable(world.getBlockState(blockPos.east().up())))) {
            return blockPos.east();
        } else if (isReplaceable(world.getBlockState(blockPos.west().up())) && (isReplaceable(world.getBlockState(blockPos.west().up())))) {
            return blockPos.west();
        } else if (isReplaceable(world.getBlockState(blockPos.south().up())) && (isReplaceable(world.getBlockState(blockPos.south().up())))) {
            return blockPos.south();
        } else if (isReplaceable(world.getBlockState(blockPos.north().up())) && (isReplaceable(world.getBlockState(blockPos.north().up())))) {
            return blockPos.north();
        }
        return null;
    }

    public static boolean has2BlocksOfPlaceToPlacePiston(ClientWorld world, BlockPos blockPos) {
        if (world.getBlockState(blockPos.up()).getHardness(world, blockPos.up()) == 0) {
            BlockBreaker.breakBlock(world, blockPos.up());
        }
        return isReplaceable(world.getBlockState(blockPos.up())) && isReplaceable(world.getBlockState(blockPos.up().up()));
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
