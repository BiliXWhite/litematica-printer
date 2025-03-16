package me.aleksilassila.litematica.printer.mixin.masa.litematicaSetConfig;


import fi.dy.masa.litematica.gui.GuiConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiConfigs.ConfigGuiTab.class,priority = 500)
public class ConfigGuiTabMixin {

    @Inject(method = "values", at = @At("RETURN"), cancellable = true, remap = false)
    private static void values(CallbackInfoReturnable<GuiConfigs.ConfigGuiTab[]> cir) {
        GuiConfigs.ConfigGuiTab[] returnValue = cir.getReturnValue();
        GuiConfigs.ConfigGuiTab[] arr = new GuiConfigs.ConfigGuiTab[returnValue.length + 1];
        System.arraycopy(returnValue, 0, arr, 0, returnValue.length);
        GuiConfigs.ConfigGuiTab myTabKey = ConfigGuiTabAccessor.init("PRINTER_TAB_KEY", arr.length, "投影打印机");
        arr[arr.length - 1] = myTabKey;
        cir.setReturnValue(arr);
    }
}
