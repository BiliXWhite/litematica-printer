package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;
//#if MC < 12002
import io.github.cottonmc.cotton.gui.widget.WCardPanel;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WCardPanel.class)
public class MixinWCardPanel {
    @Inject(at = @At("TAIL"),method = "setSelectedIndex",remap = false)
    public void setSelectedIndex(int selectedIndex, CallbackInfoReturnable<WCardPanel> cir) {
        ZxyUtils.currWorldId = selectedIndex;
    }
}
//#endif