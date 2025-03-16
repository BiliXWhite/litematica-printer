package me.aleksilassila.litematica.printer.mixin.masa.litematicaSetConfig;

import fi.dy.masa.litematica.gui.GuiConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiConfigs.ConfigGuiTab.class, remap = false)
public interface ConfigGuiTabAccessor {
    @Invoker("<init>")
    static GuiConfigs.ConfigGuiTab init(String name, int ordinal, String translationKey) {
        throw new AssertionError();
    }
}
