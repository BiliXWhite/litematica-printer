package me.aleksilassila.litematica.printer.mixin.printer.malilib;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;
import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 字符串列表标题I18n修复
@Mixin(GuiStringListEdit.class)
public abstract class MixinGuiStringListEdit extends GuiListBase<String, WidgetStringListEditEntry, WidgetListStringListEdit> {
    protected MixinGuiStringListEdit(int listX, int listY) {
        super(listX, listY);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void litematica_printer$init(IConfigStringList config, IConfigGui configGui, IDialogHandler dialogHandler, Screen parent, CallbackInfo ci) {
        this.title = StringUtils.translatable("malilib.gui.title.string_list_edit", config.getConfigGuiDisplayName()).getString();
    }
}
