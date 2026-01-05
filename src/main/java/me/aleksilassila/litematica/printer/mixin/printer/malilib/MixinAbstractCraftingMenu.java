package me.aleksilassila.litematica.printer.mixin.printer.malilib;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;

import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.utils.PinYinSearchUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// MASA 全家桶搜素增强
@Mixin(WidgetListBase.class)
public abstract class MixinAbstractCraftingMenu<TYPE, WIDGET extends WidgetListEntryBase<TYPE>> extends GuiBase {
    @Inject(method = "entryMatchesFilter", at = @At("HEAD"), cancellable = true)
    public void matchesFilter(TYPE entry, String filterText, CallbackInfoReturnable<Boolean> cir) {
        if (entry instanceof GuiConfigsBase.ConfigOptionWrapper wrapper) {
            IConfigBase config = wrapper.getConfig();
            if (config == null) {
                return;
            }
            List<String> entryStrings = new ArrayList<>();
            entryStrings.add(config.getName());
            entryStrings.add(config.getPrettyName());
            entryStrings.add(config.getConfigGuiDisplayName());
            for (String fullName : entryStrings) {
                if (fullName.contains(filterText)) {
                    cir.setReturnValue(true);
                    return;
                }
                for (String s2 : PinYinSearchUtils.getPinYin(fullName).stream().distinct().toList()) {
                    if (s2.contains(filterText)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}