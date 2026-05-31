package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import me.aleksilassila.litematica.printer.network.RemoteInventoryNetwork;
import me.aleksilassila.litematica.printer.network.handler.GetItemFromInventoryHandler;
import me.aleksilassila.litematica.printer.network.handler.ScanContainerHandler;
//#if MC >= 12005
import me.aleksilassila.litematica.printer.network.payload.GetItemFromInventoryPayload;
import me.aleksilassila.litematica.printer.network.payload.GetItemResultPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerPayload;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
//#endif
import me.aleksilassila.litematica.printer.utils.RemoteContainerUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class LitematicaPrinterMod implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        Reference.LOGGER.info("Registering remote inventory server-side handlers...");
        //#if MC >= 12005
        try {
            PayloadTypeRegistry.playC2S().register(
                GetItemFromInventoryPayload.TYPE,
                StreamCodec.ofMember(GetItemFromInventoryPayload::write, GetItemFromInventoryPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {
        }
        try {
            PayloadTypeRegistry.playS2C().register(
                GetItemResultPayload.TYPE,
                StreamCodec.ofMember(GetItemResultPayload::write, GetItemResultPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {
        }
        try {
            PayloadTypeRegistry.playC2S().register(
                ScanContainerPayload.TYPE,
                StreamCodec.ofMember(ScanContainerPayload::write, ScanContainerPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {
        }
        try {
            PayloadTypeRegistry.playS2C().register(
                ScanContainerResultPayload.TYPE,
                StreamCodec.ofMember(ScanContainerResultPayload::write, ScanContainerResultPayload::decode)
            );
        } catch (IllegalArgumentException ignored) {
        }
        //#endif
        GetItemFromInventoryHandler.register();
        ScanContainerHandler.register();
        Reference.LOGGER.info("Remote inventory server-side handlers registered.");
    }

    @Override
    public void onInitializeClient() {
        RemoteInventoryNetwork.register();
        RemoteContainerUtils.init();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
