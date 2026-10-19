package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.runtime.RuntimeAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//#if MC >= 12111
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialNetwork;
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
        // 材料列表的库存查询：注册两条通道 + 收包器；真正发包由下面的 tick 驱动
        VoidWarehouseMaterialNetwork.init();
        //#endif
        PrinterRuntime runtime = new PrinterRuntime();
        RuntimeAccess.install(runtime);
        ClientTickEvents.END_CLIENT_TICK.register(runtime::tick);
        ClientChunkEvents.CHUNK_LOAD.register(runtime.scanEngine()::invalidateChunk);
        ClientChunkEvents.CHUNK_UNLOAD.register(runtime.scanEngine()::invalidateChunk);
        //#if MC >= 12111
        // 查询只在「玩家正看着材料列表」时才真的发出去，所以放每 tick 里问一句最省心：
        // 由 VoidWarehouseMaterialNetwork 自己按刷新间隔、退避与超时决定这一 tick 要不要发包。
        ClientTickEvents.END_CLIENT_TICK.register(client -> VoidWarehouseMaterialNetwork.tick());
        //#endif
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
