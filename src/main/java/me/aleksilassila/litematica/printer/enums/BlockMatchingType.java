package me.aleksilassila.litematica.printer.enums;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.PinYinSearchUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashSet;
import java.util.Set;

public enum BlockMatchingType {
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



    public static BlockMatchingType get(BlockState requiredState, BlockState currentState, Property<?>... propertiesToIgnore) {
        Set<String> replaceSet = new HashSet<>(Configs.Print.REPLACEABLE_LIST.getStrings());

        // 如果两个方块状态完全相同，则返回正确状态
        if (requiredState == currentState) {
            return CORRECT;
        }

        // 方块相同
        if (requiredState.getBlock().equals(currentState.getBlock())) {
            // 状态不同，则返回错误状态
            if (!BlockUtils.statesEqualIgnoreProperties(requiredState, currentState, propertiesToIgnore)) {
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
        if (Configs.Print.PRINT_REPLACE.getBooleanValue() &&
                replaceSet.stream().anyMatch(string -> !PinYinSearchUtils.matchName(string, requiredState) &&
                        PinYinSearchUtils.matchName(string, currentState)) && !requiredState.isAir()
        ) {
            return MISSING_BLOCK;
        }

        // 其他情况返回错误方块状态
        return ERROR_BLOCK;
    }

    public static BlockMatchingType get(SchematicBlockContext context, Property<?>... propertiesToIgnore) {
        return get(context.requiredState, context.currentState, propertiesToIgnore);
    }

    public static BlockMatchingType get(BlockPos pos, Property<?>... propertiesToIgnore) {
        BlockState requiredState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
        BlockState currentState = Minecraft.getInstance().level.getBlockState(pos);
        return get(requiredState, currentState, propertiesToIgnore);
    }
}

