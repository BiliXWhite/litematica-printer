//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.verify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.exitGameReSet;

@Environment(EnvType.CLIENT)
@Mixin({Connection.class})
public abstract class MixinClientConnection {
    public MixinClientConnection() {
    }

    @Inject(
            method = {"disconnect*"},
            at = {@At("HEAD")}
    )
    public void disconnect(Component ignored, CallbackInfo ci) {
        exitGameReSet();
    }
}