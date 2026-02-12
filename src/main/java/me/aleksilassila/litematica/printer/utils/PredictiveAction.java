package me.aleksilassila.litematica.printer.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface PredictiveAction {
    Packet<ServerGamePacketListener> predict(int sequence);
}
