package me.aleksilassila.litematica.printer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = InputHandler.class, remap = false)
public class InputHandlerMixin {

    @WrapOperation(method = "addHotkeys", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<IConfigBase> moreHotkeys(Operation<List<ConfigHotkey>> original) {
        return LitematicaMixinMod.getHotkeyList();
    }

    @WrapOperation(method = "addKeysToMap", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<IConfigBase> moreeHotkeys(Operation<List<ConfigHotkey>> original) {
        return LitematicaMixinMod.getHotkeyList();
    }

}
