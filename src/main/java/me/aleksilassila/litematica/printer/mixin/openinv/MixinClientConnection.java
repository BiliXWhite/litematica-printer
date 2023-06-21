//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.Verify;
import me.aleksilassila.litematica.printer.printer.memory.MemoryDatabase;
import me.aleksilassila.litematica.printer.printer.utils.BreakingFlowController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;


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
        BreakingFlowController.poslist = new ArrayList<>();
        Verify.verify = null;
        MemoryDatabase database = MemoryDatabase.getCurrent();
        if (database != null) {
            MemoryDatabase.clearCurrent();
        }

    }
}