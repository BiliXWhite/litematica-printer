package me.aleksilassila.litematica.printer;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import net.fabricmc.api.ClientModInitializer;

public class ClientRegister implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OpenInventoryPacket.registerClientReceivePacket();
    }
}
