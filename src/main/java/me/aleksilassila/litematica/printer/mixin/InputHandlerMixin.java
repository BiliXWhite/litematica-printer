package me.aleksilassila.litematica.printer.mixin;

import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = InputHandler.class, remap = false)
public class InputHandlerMixin {
	
	@Redirect(method = "addHotkeys", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<ConfigHotkey> moreHotkeys() {
        return LitematicaMixinMod.getHotkeyList();
    }
	
	@Redirect(method = "addKeysToMap", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<ConfigHotkey> moreeHotkeys() {
        return LitematicaMixinMod.getHotkeyList();
    }

}
