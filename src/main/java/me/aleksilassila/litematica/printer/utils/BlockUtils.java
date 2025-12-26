package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BlockUtils {
    public static boolean isReplaceable(BlockState blockState) {
        //#if MC > 11902
        return blockState.canBeReplaced();
        //#else
        //$$ return blockState.getMaterial().isReplaceable();
        //#endif
    }

    public static @NotNull Block getBlock(ResourceLocation blockId) {
        //#if MC > 12101
        return BuiltInRegistries.BLOCK.getValue(blockId);
        //#else
        //$$ return BuiltInRegistries.BLOCK.get(blockId);
        //#endif
    }

    public static String getBlockName(Block block) {
        return block.getName().getString();
    }

    public static ResourceLocation getIdentifier(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public static String getIdentifierString(Block block) {
        return getIdentifier(block).toString();
    }

    public static boolean sideCoversSmallSquare(LevelReader levelReader, BlockPos blockPos, Direction direction) {
        return Block.canSupportCenter(levelReader, blockPos, direction);
    }
}
