package me.aleksilassila.litematica.printer.enums;

import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.minecraft.BlockStateUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public enum BlockMatchResult {
    /**
     * 缺失方块：实际位置为空，或当前方块在可替换列表中且启用了替换功能
     */
    MISSING,

    /**
     * 方块错误：方块类型完全不同，且不满足缺失/状态错误的条件
     */
    WRONG_BLOCK,

    /**
     * 状态错误：方块类型相同，但方块状态（如朝向、亮度等）不一致
     */
    WRONG_STATE,

    /**
     * 正确匹配：原理图方块与实际方块的类型和状态完全一致
     */
    CORRECT;


    public static BlockMatchResult compare(BlockState requiredState, BlockState currentState, Property<?>... propertiesToIgnore) {
        // 如果两个方块状态完全相同，则返回正确状态
        if (requiredState == currentState) {
            return CORRECT;
        }
        // 方块相同
        if (requiredState.getBlock().equals(currentState.getBlock())) {
            // 状态不同，则返回错误状态
            if (BlockStateUtils.statesEqualIgnoreProperties(requiredState, currentState, propertiesToIgnore)) {
                return CORRECT;
            }
            return WRONG_STATE;
        }
        // 如果原理图中方块不为空，且实际方块为空，则返回缺失方块状态
        if (!requiredState.isAir() && currentState.isAir()) {
            return MISSING;

        }
        return WRONG_BLOCK;
    }

    public static BlockMatchResult compare(SchematicBlockContext context, Property<?>... propertiesToIgnore) {
        return compare(context.requiredState, context.currentState, propertiesToIgnore);
    }
}

