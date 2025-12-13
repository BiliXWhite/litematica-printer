package me.aleksilassila.litematica.printer;

import fi.dy.masa.malilib.event.InitializationHandler;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LitematicaPrinterMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "litematica_printer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 👉 服务端+客户端通用逻辑（仅放无客户端依赖的代码）
    // 例如：注册网络包、通用配置加载（无GUI）、数据生成等
    @Override
    public void onInitialize() {
        OpenInventoryPacket.init();
        OpenInventoryPacket.registerReceivePacket();
    }

    // 👉 仅客户端逻辑（放心使用客户端API）
    // 比如：注册按键、GUI、渲染钩子、客户端配置界面等
    @Override
    public void onInitializeClient() {
        OpenInventoryPacket.registerClientReceivePacket();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
