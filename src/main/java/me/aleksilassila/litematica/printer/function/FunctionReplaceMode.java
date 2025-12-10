package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;


public class FunctionReplaceMode extends FunctionModeBase {
    private final BreakManager breakManager = BreakManager.instance();

    @Override
    public State.PrintModeType getPrintModeType() {
        return State.PrintModeType.REPLACE;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return InitHandler.REPLACE_BLOCK;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {

    }

}
