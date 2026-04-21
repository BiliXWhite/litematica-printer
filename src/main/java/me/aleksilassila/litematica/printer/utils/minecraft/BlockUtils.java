package me.aleksilassila.litematica.printer.utils.minecraft;

import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockUtils {
    public static boolean checkObserverChain(SchematicBlockContext start) {
        SchematicBlockContext temp = start;
        while (temp.requiredState.getBlock() instanceof ObserverBlock) {
            @Nullable
            Direction tempObserverFacing = temp.requiredProperty(ObserverBlock.FACING).orElse(null);    
            SchematicBlockContext offset = temp.offset(tempObserverFacing);
            if (tempObserverFacing != null && BlockMatchResult.compare(offset) != BlockMatchResult.CORRECT) {
                return false;
            }
            temp = offset;
        }
        return true;
    }

    public static boolean isReplaceable(BlockState blockState) {
        //#if MC > 11902
        return blockState.canBeReplaced();
        //#else
        //$$ return blockState.getMaterial().isReplaceable();
        //#endif
    }

    public static @NotNull Block getBlock(Identifier blockId) {
        //#if MC > 12101
        return BuiltInRegistries.BLOCK.getValue(blockId);
        //#else
        //$$ return BuiltInRegistries.BLOCK.get(blockId);
        //#endif
    }

    public static String getBlockName(Block block) {
        return block.getName().getString();
    }

    public static Identifier getKey(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public static String getKeyString(Block block) {
        return getKey(block).toString();
    }

    public static boolean canSupportCenter(LevelReader levelReader, BlockPos blockPos, Direction direction) {
        return Block.canSupportCenter(levelReader, blockPos, direction);
    }
}
