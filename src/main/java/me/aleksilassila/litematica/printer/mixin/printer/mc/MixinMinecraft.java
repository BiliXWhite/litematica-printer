package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.utils.ModUtils;
//#if MC >= 260200
//$$ import net.minecraft.client.gui.Gui;
//#else
import net.minecraft.client.Minecraft;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260200
//$$ @Mixin(Gui.class)
//#else
@Mixin(Minecraft.class)
//#endif
public class MixinMinecraft {
    //shit. In mapping-26.1.2-26.2.txt replaces setScreen to screen
    //#if MC >= 260200
    //$$ @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    //#endif
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (ModUtils.closeScreen > 0 && screen instanceof AbstractContainerScreen<?>) {
            ModUtils.closeScreen--;
            ci.cancel();
        }
    }
}
