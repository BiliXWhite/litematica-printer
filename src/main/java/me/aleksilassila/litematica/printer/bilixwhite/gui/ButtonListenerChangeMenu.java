package me.aleksilassila.litematica.printer.bilixwhite.gui;

import me.aleksilassila.litematica.printer.config.ConfigUI;
import net.minecraft.client.gui.screens.Screen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;

import java.util.Objects;

public record ButtonListenerChangeMenu(ButtonType type, Screen parent) implements IButtonActionListener {

    @Override
    public void actionPerformedWithButton(final ButtonBase arg0, final int arg1) {
        if (Objects.requireNonNull(type) == ButtonType.PRINTER_SETTINGS) {
            GuiBase.openGui(new ConfigUI().setParent(parent));
        }
    }
}