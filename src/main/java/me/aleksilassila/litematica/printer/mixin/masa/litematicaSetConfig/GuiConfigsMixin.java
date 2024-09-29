package me.aleksilassila.litematica.printer.mixin.masa.litematicaSetConfig;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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


    @WrapOperation(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Colors;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<IConfigBase> colorsOptions(Operation<ImmutableList<IConfigBase>> original) {
        return LitematicaMixinMod.getColorsList();
    }
    @WrapOperation(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<IConfigBase> moreOptions(Operation<ImmutableList<IConfigBase>> original) {
        return LitematicaMixinMod.getConfigList();
    }

    @WrapOperation(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<IConfigBase> moreHotkeys(Operation<List<ConfigHotkey>> original) {
        return LitematicaMixinMod.getHotkeyList();
    }
}
