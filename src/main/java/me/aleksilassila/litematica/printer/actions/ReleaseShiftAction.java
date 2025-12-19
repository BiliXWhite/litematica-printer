package me.aleksilassila.litematica.printer.actions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ReleaseShiftAction extends Action {
    @Override
    public void send(Minecraft client, LocalPlayer player) {
        PrepareAction.sendShift(player, false);
    }
}