package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin({ClientConnection.class})
public abstract class MixinClientConnection {
    public MixinClientConnection() {
    }

    @Inject(
            method = {"disconnect"},
            at = {@At("HEAD")}
    )
    public void chestTracker$onDisconnectHandler(Text ignored, CallbackInfo ci) {
        if(!LitematicaMixinMod.INVENTORY.getBooleanValue()) return;
        MemoryDatabase database = MemoryDatabase.getCurrent();
        if (database != null) {
            MemoryDatabase.clearCurrent();
        }
    }
}
