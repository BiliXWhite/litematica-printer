package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import me.aleksilassila.litematica.printer.network.RemoteInventoryNetwork;
//#if MC >= 12005
import me.aleksilassila.litematica.printer.network.handler.RemoteExchangeHandler;
import me.aleksilassila.litematica.printer.network.handler.ScanContainerHandler;
import me.aleksilassila.litematica.printer.network.payload.RemoteExchangePayload;
import me.aleksilassila.litematica.printer.network.payload.RemoteExchangeResultPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
//#endif
import me.aleksilassila.litematica.printer.container.ContainerCachePersister;
import me.aleksilassila.litematica.printer.utils.RemoteContainerUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class LitematicaPrinterMod implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        //#if MC >= 12005
        try {
            PayloadTypeRegistry.playC2S().register(
                RemoteExchangePayload.TYPE,
                StreamCodec.ofMember(RemoteExchangePayload::write, RemoteExchangePayload::decode)
            );
        } catch (IllegalArgumentException ignored) {}
        try {
            PayloadTypeRegistry.playS2C().register(
                RemoteExchangeResultPayload.TYPE,
                StreamCodec.ofMember(RemoteExchangeResultPayload::write, RemoteExchangeResultPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {}
        try {
            PayloadTypeRegistry.playC2S().register(
                ScanContainerPayload.TYPE,
                StreamCodec.ofMember(ScanContainerPayload::write, ScanContainerPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {}
        try {
            PayloadTypeRegistry.playS2C().register(
                ScanContainerResultPayload.TYPE,
                StreamCodec.ofMember(ScanContainerResultPayload::write, ScanContainerResultPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {}
        RemoteExchangeHandler.register();
        ScanContainerHandler.register();
        //#endif
    }

    @Override
    public void onInitializeClient() {
        RemoteInventoryNetwork.register();
        RemoteContainerUtils.init();
        ContainerCachePersister.register();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
