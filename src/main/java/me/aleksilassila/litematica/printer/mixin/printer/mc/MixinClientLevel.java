package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.mixin.extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.utils.PacketUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel implements PacketUtils.SequenceExtension {

    //#if MC > 11802
    @Final
    @Shadow
    private net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler blockStatePredictionHandler;

    @Override
    public void litematica_printer$sendPrediction(MultiPlayerGameModeExtension.PredictiveAction action) {
        // 预测上下文需覆盖本地世界修改和发包，服务端拒绝时才能正确回滚。
        try (net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler prediction = blockStatePredictionHandler.startPredicting()) {
            PacketUtils.sendPacket(action.predict(prediction.currentSequence()));
        }
    }
    //#endif
}
