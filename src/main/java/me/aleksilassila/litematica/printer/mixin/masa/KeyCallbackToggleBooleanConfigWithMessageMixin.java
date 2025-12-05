package me.aleksilassila.litematica.printer.mixin.masa;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBoolean;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import me.aleksilassila.litematica.printer.printer.State;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.MODE_SWITCH;

@Mixin(KeyCallbackToggleBooleanConfigWithMessage.class)
public class KeyCallbackToggleBooleanConfigWithMessageMixin extends KeyCallbackToggleBoolean {

    public KeyCallbackToggleBooleanConfigWithMessageMixin(IConfigBoolean config) {
        super(config);
    }

    @Inject(at = @At("HEAD"),method = "onKeyAction",remap = false, cancellable = true)
    public void onKeyAction(KeyAction action, IKeybind key, CallbackInfoReturnable<Boolean> cir){
        String name = config.getName();
        //TODO: 暂时不太清楚是什么, 硬编码的？
        if (MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE) &&
                (name.equals("挖掘") || name.equals("排流体") || name.equals("填充") || name.equals("破基岩"))) {
            cir.setReturnValue(false);
        }
    }
}

