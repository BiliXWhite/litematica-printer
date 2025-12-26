package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public enum State {
    MISSING_BLOCK,
    WRONG_STATE,
    WRONG_BLOCK,
    CORRECT;

    public static State get(BlockState schematicBlockState, BlockState currentBlockState) {
        Set<String> replaceSet = new HashSet<>(InitHandler.REPLACEABLE_LIST.getStrings());
        // 如果两个方块状态完全相同，则返回正确状态
        if (schematicBlockState == currentBlockState)
            return CORRECT;
            // 如果方块类型相同但状态不同，则返回错误状态
        else if (schematicBlockState.getBlock().defaultBlockState() == currentBlockState.getBlock().defaultBlockState())
            return WRONG_STATE;
            // 如果原理图中方块不为空，且实际方块为空，则返回缺失方块状态
        else if (!schematicBlockState.isAir() && currentBlockState.isAir())
            return MISSING_BLOCK;
            // 如果启用了替换功能，且当前方块在可替换列表中，则返回缺失方块状态（实际上这会和破坏额外方块打架）
        else if (InitHandler.REPLACE.getBooleanValue() &&
                replaceSet.stream().anyMatch(string -> !Filters.equalsName(string, schematicBlockState) &&
                        Filters.equalsName(string, currentBlockState)) && !schematicBlockState.isAir()
        ) return MISSING_BLOCK;
            // 其他情况返回错误方块状态
        else return WRONG_BLOCK;
    }

    public static State get(BlockPos pos) {
        BlockState schematicBlockState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
        BlockState currentBlockState = Minecraft.getInstance().level.getBlockState(pos);
        return get(schematicBlockState, currentBlockState);
    }
}

