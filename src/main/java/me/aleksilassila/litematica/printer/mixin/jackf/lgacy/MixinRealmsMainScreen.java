//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;
//#if MC < 12001
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//$$ import net.fabricmc.api.EnvType;
//$$ import net.fabricmc.api.Environment;
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import net.minecraft.client.realms.dto.RealmsServer;
//$$ import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Environment(EnvType.CLIENT)
//$$ @Mixin({RealmsMainScreen.class})
//$$ public class MixinRealmsMainScreen {
//$$     public MixinRealmsMainScreen() {
//$$     }
//$$
//$$     @Inject(
//$$             method = {"play"},
//$$             at = {@At("HEAD")}
//$$     )
//$$     private void chestTracker$saveLastConnectedServer(RealmsServer realmsServer, Screen screen, CallbackInfo ci) {
//$$         MemoryUtils.setLastRealmsServer(realmsServer);
//$$     }
//$$ }
//#endif