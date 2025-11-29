
package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({RealmsMainScreen.class})
public class MixinRealmsMainScreen {
    public MixinRealmsMainScreen() {
    }

    @Inject(
            method = {"play"},
            at = {@At("HEAD")}
    )
    private void chestTracker$saveLastConnectedServer(RealmsServer realmsServer, Screen screen, CallbackInfo ci) {
        MemoryUtils.setLastRealmsServer(realmsServer);
    }
}
