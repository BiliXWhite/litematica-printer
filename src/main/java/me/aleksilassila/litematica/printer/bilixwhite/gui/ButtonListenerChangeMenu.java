package me.aleksilassila.litematica.printer.bilixwhite.gui;

import me.aleksilassila.litematica.printer.config.ConfigUi;
import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;

import java.util.Objects;

public class ButtonListenerChangeMenu implements IButtonActionListener
{
    private final ButtonType type;
    private final Screen parent;

    public ButtonListenerChangeMenu(final ButtonType type, final Screen parent)
    {
        this.type = type;
        this.parent = parent;
    }

    @Override
    public void actionPerformedWithButton(final ButtonBase arg0, final int arg1)
    {
        if (Objects.requireNonNull(type) == ButtonType.PRINTER_SETTINGS) {
            GuiBase.openGui(new ConfigUi().setParent(parent));
        }
    }
}