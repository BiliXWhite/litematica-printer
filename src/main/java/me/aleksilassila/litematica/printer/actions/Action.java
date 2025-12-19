package me.aleksilassila.litematica.printer.actions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public abstract class Action {
    abstract public void send(Minecraft client, LocalPlayer player);
}