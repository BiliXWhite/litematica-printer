package me.aleksilassila.litematica.printer.hotkeys;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBoolean;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.StringUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public class KeyCallbackToggleBooleanConfigWithMessage extends KeyCallbackToggleBoolean {
    public KeyCallbackToggleBooleanConfigWithMessage(IConfigBoolean config) {
        super(config);
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        super.onKeyAction(action, key);
        printBooleanConfigToggleMessage(this.config.getName(), this.config.getBooleanValue());
        return true;
    }

    private static void printBooleanConfigToggleMessage(String name, boolean newValue) {
//        I18n i18n = I18n.get(name);
//        if (i18n != null) {
//            name = StringUtils.translatable(i18n.getFullKey()).getString();
//        }
        String pre = newValue ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        String status = pre + (newValue ? I18n.MESSAGE_VALUE_ON : I18n.MESSAGE_VALUE_OFF).getBaseString() + GuiBase.TXT_RST;
        String message = String.format(I18n.MESSAGE_TOGGLED.getBaseString(), StringUtils.translatable(name).getString(), status);
        Minecraft.getInstance().gui.setOverlayMessage(StringUtils.literal(message), false);
    }
}
