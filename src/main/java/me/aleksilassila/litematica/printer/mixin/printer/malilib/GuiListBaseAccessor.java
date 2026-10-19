package me.aleksilassila.litematica.printer.mixin.printer.malilib;

//#if MC >= 12111
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Refresh an existing material browser without recreating the GUI's input fields. */
@Mixin(value = GuiListBase.class, remap = false)
public interface GuiListBaseAccessor {
    @Accessor("widget")
    WidgetListBase<?, ?> litematicaPrinter$getListWidget();
}
//#endif
