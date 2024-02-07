package me.aleksilassila.litematica.printer.mixin;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = GuiConfigs.class, remap = false)
public class GuiConfigsMixin {
	
	/*@Overwrite
	public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs;
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.GENERIC)
        {
            configs = LitematicaMixinMod.betterConfigList;
        }
        else if (tab == ConfigGuiTab.INFO_OVERLAYS)
        {
            configs = Configs.InfoOverlays.OPTIONS;
        }
        else if (tab == ConfigGuiTab.VISUALS)
        {
            configs = Configs.Visuals.OPTIONS;
        }
        else if (tab == ConfigGuiTab.COLORS)
        {
            configs = Configs.Colors.OPTIONS;
        }
        else if (tab == ConfigGuiTab.HOTKEYS)
        {
            configs = LitematicaMixinMod.betterHotkeyList;
        }
        else
        {
            return Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(configs);
    }*/
	
	
    @Redirect(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<IConfigBase> moreOptions() {
        return LitematicaMixinMod.getConfigList();
    }
    
    @Redirect(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<IConfigBase> moreHotkeys() {
        return LitematicaMixinMod.getHotkeyList();
    }
}
