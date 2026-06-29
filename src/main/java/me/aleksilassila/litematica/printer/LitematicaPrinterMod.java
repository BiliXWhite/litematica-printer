package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import me.aleksilassila.litematica.printer.utils.RemoteContainerUtils;
import net.fabricmc.api.ClientModInitializer;

public class LitematicaPrinterMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RemoteContainerUtils.init();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
