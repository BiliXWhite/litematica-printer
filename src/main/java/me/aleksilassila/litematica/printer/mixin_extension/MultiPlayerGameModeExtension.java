package me.aleksilassila.litematica.printer.mixin_extension;

import me.aleksilassila.litematica.printer.utils.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("UnusedReturnValue")
public interface MultiPlayerGameModeExtension {
    InteractionResult fabric_bedrock_miner$useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit);

    BlockBreakResult litematica_printer$continueDestroyBlock(boolean localPrediction, BlockPos blockPos, Direction direction);

    void fabric_bedrock_miner$startPrediction(PredictiveAction predictiveAction);

    BlockPos fabric_bedrock_miner$destroyBlockPos();

    boolean fabric_bedrock_miner$isDestroying();
}