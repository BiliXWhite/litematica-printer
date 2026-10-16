package me.aleksilassila.litematica.printer.guide.guides;

import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.guide.Guide;
import me.aleksilassila.litematica.printer.guide.Result;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;
import me.aleksilassila.litematica.printer.utils.minecraft.DirectionUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;

/**
 * 头颅
 */
public class SkullGuide extends Guide {

    public SkullGuide(SchematicBlockContext context) {
        super(context);
    }

    @Override
    protected Result onBuildActionMissingBlock(BlockMatchResult state) {
        Direction facing = getProperty(requiredState, WallSkullBlock.FACING).orElse(null);

        if (requiredBlock instanceof SkullBlock) {
            int rotation = getProperty(requiredState, SkullBlock.ROTATION).orElseThrow();
            return Result.success(new Action()
                    .setSides(Direction.DOWN)
                    .setLookRotation(DirectionUtils.getOppositeRotation(rotation))
                    .setRequiresSupport());
        }
        if (requiredBlock instanceof WallSkullBlock && facing != null) {
            return Result.success(new Action()
                    .setSides(facing.getOpposite())
                    .setLookDirection(facing.getOpposite())
                    .setRequiresSupport());
        }
        return Result.SKIP;
    }
}
