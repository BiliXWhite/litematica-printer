//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.verify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.exitGameReSet;


// TODO(Ravel): can not resolve target class ClientConnection
// TODO(Ravel): can not resolve target class ClientConnection
@Environment(EnvType.CLIENT)
@Mixin({ClientConnection.class})
public abstract class MixinClientConnection {
    public MixinClientConnection() {
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(
            method = {"disconnect*"},
            at = {@At("HEAD")}
    )
    public void disconnect(Text ignored, CallbackInfo ci) {
        exitGameReSet();
    }
}