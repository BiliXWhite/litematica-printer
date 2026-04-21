package me.aleksilassila.litematica.printer.guide;

import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;
import me.aleksilassila.litematica.printer.utils.minecraft.BlockStateUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultGuide extends Guide {
    public DefaultGuide(SchematicBlockContext context) {
        super(context);
    }

    @Override
    protected Optional<Action> onBuildActionMissingBlock(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        Action action = new Action();

        // 处理 FACING 方块
        BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.FACING)
                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.HORIZONTAL_FACING))
                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.VERTICAL_DIRECTION))
                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.FACING_HOPPER))
                .ifPresent(facing -> action.setLookDirection(facing.getOpposite()));

        // 处理 AXIS 轴方块
        BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.AXIS)
                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.HORIZONTAL_AXIS))
                .ifPresent(axis -> action.setLookDirection(
                        switch (axis) {
                            case X -> Direction.EAST;
                            case Y -> Direction.UP;
                            case Z -> Direction.SOUTH;
                        })
                );


        return super.onBuildActionMissingBlock(state, skipOtherGuide);
    }
}