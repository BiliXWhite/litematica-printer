package me.aleksilassila.litematica.printer.mixin;

import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.malilib.config.IConfigBase;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = InputHandler.class, remap = false)
public class InputHandlerMixin {
	
	@Redirect(method = "addHotkeys", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<IConfigBase> moreHotkeys() {
        return LitematicaMixinMod.getHotkeyList();
    }
	
	@Redirect(method = "addKeysToMap", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<IConfigBase> moreeHotkeys() {
        return LitematicaMixinMod.getHotkeyList();
    }

}
