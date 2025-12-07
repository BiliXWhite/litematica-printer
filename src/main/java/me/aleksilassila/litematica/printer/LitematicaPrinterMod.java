package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class LitematicaPrinterMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "litematica_printer";

    @Override
    public void onInitialize() {
    }

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
