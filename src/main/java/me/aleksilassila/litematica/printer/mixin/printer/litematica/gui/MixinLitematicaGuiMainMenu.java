package me.aleksilassila.litematica.printer.mixin.printer.litematica.gui;

import me.aleksilassila.litematica.printer.gui.ButtonListenerChangeMenu;
import me.aleksilassila.litematica.printer.gui.ButtonType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.gui.GuiMainMenu;

@Mixin(GuiMainMenu.class)
public abstract class MixinLitematicaGuiMainMenu extends GuiBase {

    @Invoker(remap = false)
    public abstract int callGetButtonWidth();

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    public void initGui(final CallbackInfo ci) {
        final int width = callGetButtonWidth();
        final int x = width + 12 + 20;
        int y = 30 + 22;
        createPrinterSettingsButton(x, y, width, ButtonType.PRINTER_SETTINGS);
    }

    @Unique
    private ButtonGeneric createPrinterSettingsButton(final int x, final int y, final int width, ButtonType type) {
        final ButtonGeneric button = new ButtonGeneric(x, y, width, 20, type.getDisplayName());
        button.setEnabled(true);
        addButton(button, new ButtonListenerChangeMenu(type, this));
        return button;
    }
}