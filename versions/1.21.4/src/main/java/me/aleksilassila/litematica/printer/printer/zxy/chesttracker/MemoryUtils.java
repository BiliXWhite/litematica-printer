package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import red.jackf.chesttracker.api.memory.Memory;
import red.jackf.chesttracker.api.memory.MemoryBank;
import red.jackf.chesttracker.api.providers.MemoryBuilder;
import red.jackf.chesttracker.api.providers.ProviderUtils;
import red.jackf.chesttracker.impl.events.AfterPlayerDestroyBlock;
import red.jackf.chesttracker.impl.memory.MemoryBankAccessImpl;
import red.jackf.chesttracker.impl.memory.MemoryBankImpl;
import red.jackf.chesttracker.impl.memory.metadata.Metadata;
import red.jackf.chesttracker.impl.storage.ConnectionSettings;
import red.jackf.chesttracker.impl.storage.Storage;
import red.jackf.jackfredlib.client.api.gps.Coordinate;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.api.search.ConnectedBlocksGrabber;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MemoryUtils {
    @NotNull
    static Minecraft client = Minecraft.getInstance();

    public static MemoryBankImpl PRINTER_MEMORY = null;
//    public static MemoryBankImpl memoryBankImpl = MemoryBankAccessImpl.INSTANCE.getLoadedInternal().get();

    //点击的物品
    public static ItemStack itemStack = null;
    //当前打开的维度
    public static ResourceLocation currentMemoryKey = null;
    //远程取物返回包中的方块数据
    public static BlockState blockState = null;
    //箱子追踪搜索请求
    public static SearchRequest request = null;
    //打印机库存设置
    public static Metadata printerMetadata = null;

    public static void deletePrinterMemory() {
        if (PRINTER_MEMORY != null) {
            String id = PRINTER_MEMORY.getId();
            printerMetadata = PRINTER_MEMORY.getMetadata();
            unLoad();
            Storage.delete(id);
            createPrinterMemory();
        }
        client.gui.setOverlayMessage(Component.nullToEmpty("打印机库存已清空"), false);
    }

    public static void setup() {
        //破坏方块后清除打印机库存的该记录
        AfterPlayerDestroyBlock.EVENT.register(cbs -> {
            if (PRINTER_MEMORY != null
                    && PRINTER_MEMORY.getMetadata().getIntegritySettings().removeOnPlayerBlockBreak
            ) {
                ProviderUtils.getPlayersCurrentKey().ifPresent(currentKey -> PRINTER_MEMORY.removeMemory(currentKey, cbs.pos()));
            }
        });

        //关闭屏幕后保存 在屏蔽掉ui的情况下 这里可能无法触发 建议在mixin中调用保存方法
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> sc) {
                ScreenEvents.remove(screen).register(screen1 -> {
//                    saveMemory(sc.getScreenHandler());
                });
            }
        });

        //加载打印机库存
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(MemoryUtils::createPrinterMemory));
        //保存打印机库存
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> unLoad());
    }
    public static void saveMemory(AbstractContainerMenu sc){
        if(PRINTER_MEMORY != null && ZxyUtils.printerMemoryAdding || Printer.getInstance().printerMemorySync)
            save(sc , PRINTER_MEMORY);
        MemoryBankAccessImpl.INSTANCE.getLoadedInternal().ifPresent(memoryBank -> save(sc, memoryBank));
        Printer.getInstance().printerMemorySync = false;
    }
    public static void createPrinterMemory(){
        Optional<Coordinate> current = Coordinate.getCurrent();
        if (current.isPresent()) {
            Coordinate coordinate = current.get();
            String s1 = coordinate.id() + "-printer";

            ConnectionSettings orCreate = ConnectionSettings.getOrCreate(s1);
            String s = orCreate.memoryBankIdOverride().orElse(s1);

            unLoad();
            PRINTER_MEMORY = Storage.load(s).orElseGet(() -> {
                var bank = new MemoryBankImpl(Metadata.blankWithName(coordinate.userFriendlyName() + "-printer"), new HashMap<>());
                bank.setId(s1);
                return bank;
            });
            if(printerMetadata != null){
                PRINTER_MEMORY.setMetadata(printerMetadata);
            }
//            Metadata metadata = PRINTER_MEMORY.getMetadata();
//            SearchSettings searchSettings = metadata.getSearchSettings();
//            searchSettings.searchRange = Integer.MAX_VALUE;
//            searchSettings.itemListRange = Integer.MAX_VALUE;
//            metadata.getIntegritySettings().memoryLifetime = NEVER;
            save();
        }
    }

    public static void unLoad() {
        if (PRINTER_MEMORY != null) {
            if (MemoryBankAccessImpl.INSTANCE.getLoadedInternal().isPresent() && MemoryBankAccessImpl.INSTANCE.getLoadedInternal().get().equals(PRINTER_MEMORY)) {
                MemoryBankAccessImpl.INSTANCE.unload();
            }
            save();
        }
        PRINTER_MEMORY = null;
    }

    public static void save() {
        Storage.save(PRINTER_MEMORY);
    }

    public static void save(AbstractContainerMenu screen , MemoryBank memoryBank) {
        if (memoryBank == null || OpenInventoryPacket.key == null || blockState == null || !Configs.Core.CLOUD_INVENTORY.getBooleanValue()) return;
        List<BlockPos> connected;
        if (ZxyUtils.printerMemoryAdding && client.level != null) {
            connected = ConnectedBlocksGrabber.getConnected(client.level, client.level.getBlockState(OpenInventoryPacket.pos), OpenInventoryPacket.pos);
        } else connected = null;
        List<ItemStack> items;
        if (screen !=null)
            items = screen.slots.stream()
                    .filter(slot -> !(slot.container instanceof Inventory))
                    .map(Slot::getItem)
                    .toList();
        else return;

        Memory memory = MemoryBuilder.create(items)
                .inContainer(blockState.getBlock())
                .otherPositions(connected != null ? connected.stream()
                        .filter(pos -> !pos.equals(connected.get(0)))
                        .toList() : List.of(OpenInventoryPacket.pos)
                ).build();
        if (memory != null) {
            memoryBank.addMemory(OpenInventoryPacket.key.location(),OpenInventoryPacket.pos,memory);
        }
    }
}
//#endif