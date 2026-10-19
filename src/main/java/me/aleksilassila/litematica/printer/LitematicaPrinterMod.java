package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//#if MC >= 12111
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseRestockNetwork;
//#endif

public class LitematicaPrinterMod implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
    }

    @Override
    public void onInitializeClient() {
        //#if MC >= 12111
        // 必须在这一步注册：再晚就来不及和服务端协商通道了
        VoidWarehouseRestockNetwork.init();
        //#endif
        PrinterRuntime runtime = new PrinterRuntime();
        RuntimeAccess.install(runtime);
        ClientTickEvents.END_CLIENT_TICK.register(runtime::tick);
        ClientChunkEvents.CHUNK_LOAD.register(runtime.scanEngine()::invalidateChunk);
        ClientChunkEvents.CHUNK_UNLOAD.register(runtime.scanEngine()::invalidateChunk);
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
