package me.aleksilassila.litematica.printer.mixin.masa.litematicaSetConfig;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.config.ConfigUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;

@Mixin(value = GuiConfigs.class, remap = false)
public abstract class GuiConfigsMixin {

    @Unique
    private static GuiConfigs.ConfigGuiTab tempTab = GuiConfigs.ConfigGuiTab.GENERIC;

    @WrapOperation(at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/gui/GuiConfigs;createButton(IIILfi/dy/masa/litematica/gui/GuiConfigs$ConfigGuiTab;)I", ordinal = 5), method = "initGui")
    private int createMyButton(GuiConfigs instance, int x, int y, int width, GuiConfigs.ConfigGuiTab tab, Operation<Integer> original) {
        Integer call = original.call(instance, x, y, width, tab);
        createButton(call + x, y-20 ,-1 , LitematicaMixinMod.PRINTER_TAB_KEY);
        return call;
    }
    @Inject(at =@At("HEAD"),method = "getConfigs", cancellable = true,remap = false)
    private void getConfigs(CallbackInfoReturnable<List<GuiConfigsBase.ConfigOptionWrapper>> cir) {
        GuiConfigs.ConfigGuiTab tab = DataManager.getConfigGuiTab();
        if (LitematicaMixinMod.PRINTER_TAB_KEY.equals(tab)) {
            client.setScreen(new ConfigUi());
            DataManager.setConfigGuiTab(tempTab);
            cir.setReturnValue(GuiConfigsBase.ConfigOptionWrapper.createFor(Collections.emptyList()));
        }else tempTab = tab;
    }

    @Shadow(remap = false)
    protected abstract int createButton(int x, int y, int width, GuiConfigs.ConfigGuiTab tab);


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
