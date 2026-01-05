package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.utils.NetworkUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel implements NetworkUtils.SequenceExtension {

    //#if MC > 11802
    @Final
    @Shadow
    private net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler blockStatePredictionHandler;

    @Override
    public int litematica_printer3$getSequence() {
        try (net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler pendingUpdateManager = blockStatePredictionHandler) {
            return pendingUpdateManager.currentSequence();
        }
    }
    //#endif
}
