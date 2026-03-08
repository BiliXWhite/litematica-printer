package me.aleksilassila.litematica.printer.mixin_extension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("UnusedReturnValue")
public interface MultiPlayerGameModeExtension {
    InteractionResult litematica_printer$useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit);

    BlockBreakResult litematica_printer$continueDestroyBlock(boolean localPrediction, BlockPos blockPos, Direction direction);

    void litematica_printer$startPrediction(PredictiveAction predictiveAction);

    BlockPos litematica_printer$destroyBlockPos();

    boolean litematica_printer$isDestroying();

    @FunctionalInterface
    interface PredictiveAction {
        Packet<ServerGamePacketListener> predict(int sequence);
    }
}