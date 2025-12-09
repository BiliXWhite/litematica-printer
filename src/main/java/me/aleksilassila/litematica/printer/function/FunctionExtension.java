package me.aleksilassila.litematica.printer.function;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public interface FunctionExtension {
    void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player);
}
