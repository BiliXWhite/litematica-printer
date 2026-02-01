package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import me.aleksilassila.litematica.printer.utils.BlockStateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public enum State {
    /**
     * 缺失方块：实际位置为空，或当前方块在可替换列表中且启用了替换功能
     */
    MISSING_BLOCK,

    /**
     * 方块错误：方块类型完全不同，且不满足缺失/状态错误的条件
     */
    ERROR_BLOCK,

    /**
     * 状态错误：方块类型相同，但方块状态（如朝向、亮度等）不一致
     */
    ERROR_BLOCK_STATE,

    /**
     * 正确匹配：原理图方块与实际方块的类型和状态完全一致
     */
    CORRECT;

    private final static BooleanProperty wallUpProperty = WallBlock.UP;
    //#if MC > 12104
    private final static EnumProperty<WallSide> wallNorthProperty = WallBlock.NORTH;
    private final static EnumProperty<WallSide> wallSouthProperty = WallBlock.SOUTH;
    private final static EnumProperty<WallSide> wallWestProperty = WallBlock.WEST;
    private final static EnumProperty<WallSide> wallEastProperty = WallBlock.EAST;
    //#else
    //$$ private final static EnumProperty<WallSide> wallNorthProperty = WallBlock.NORTH_WALL;
    //$$ private final static EnumProperty<WallSide> wallSouthProperty = WallBlock.SOUTH_WALL;
    //$$ private final static EnumProperty<WallSide> wallWestProperty = WallBlock.WEST_WALL;
    //$$ private final static EnumProperty<WallSide> wallEastProperty = WallBlock.EAST_WALL;
    //#endif

    public static State get(BlockState requiredState, BlockState currentState, Property<?>... propertiesToIgnore) {
        Set<String> replaceSet = new HashSet<>(Configs.Print.REPLACEABLE_LIST.getStrings());

        // 如果两个方块状态完全相同，则返回正确状态
        if (requiredState == currentState) {
            return CORRECT;
        }

        // 方块相同
        if (requiredState.getBlock().equals(currentState.getBlock())) {
            // 状态不同，则返回错误状态
            if (!BlockStateUtils.statesEqualIgnoreProperties(requiredState, currentState, propertiesToIgnore)) {
                return ERROR_BLOCK_STATE;
            }
        }

        // 如果原理图中方块不为空，且实际方块为空，则返回缺失方块状态
        if (!requiredState.isAir() && !requiredState.is(Blocks.AIR) && !requiredState.is(Blocks.CAVE_AIR) && !currentState.is(Blocks.VOID_AIR)) {
            if (currentState.isAir() || currentState.is(Blocks.AIR) || currentState.is(Blocks.CAVE_AIR) || currentState.is(Blocks.VOID_AIR)) {
                return MISSING_BLOCK;
            }
        }

        // 如果启用了替换功能，且当前方块在可替换列表中，则返回缺失方块状态（实际上这会和破坏额外方块打架）
        if (Configs.Print.REPLACE.getBooleanValue() &&
                replaceSet.stream().anyMatch(string -> !FilterUtils.matchName(string, requiredState) &&
                        FilterUtils.matchName(string, currentState)) && !requiredState.isAir()
        ) {
            return MISSING_BLOCK;
        }

        // 其他情况返回错误方块状态
        return ERROR_BLOCK;
    }

    public static State get(BlockContext context, Property<?>... propertiesToIgnore) {
        return get(context.requiredState, context.currentState, propertiesToIgnore);
    }

    public static State get(BlockPos pos, Property<?>... propertiesToIgnore) {
        BlockState requiredState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
        BlockState currentState = Minecraft.getInstance().level.getBlockState(pos);
        return get(requiredState, currentState, propertiesToIgnore);
    }

    public static Optional<Property<?>> getWallFacingProperty(Direction wallFacing) {
        switch (wallFacing) {
            case UP -> {
                return Optional.of(wallUpProperty);
            }
            case NORTH -> {
                return Optional.of(wallNorthProperty);
            }
            case SOUTH -> {
                return Optional.of(wallSouthProperty);
            }
            case WEST -> {
                return Optional.of(wallWestProperty);
            }
            case EAST -> {
                return Optional.of(wallEastProperty);
            }
        }
        return Optional.empty();
    }

    public static Optional<Property<?>> getCrossCollisionBlock(Direction wallFacing) {
        switch (wallFacing) {
            case NORTH -> {
                return Optional.of(wallNorthProperty);
            }
            case SOUTH -> {
                return Optional.of(wallSouthProperty);
            }
            case WEST -> {
                return Optional.of(wallWestProperty);
            }
            case EAST -> {
                return Optional.of(wallEastProperty);
            }
        }
        return Optional.empty();
    }
}

