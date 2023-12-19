package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import red.jackf.chesttracker.api.events.AfterPlayerDestroyBlock;
import red.jackf.chesttracker.api.provider.MemoryBuilder;
import red.jackf.chesttracker.api.provider.ProviderUtils;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.chesttracker.memory.metadata.Metadata;
import red.jackf.chesttracker.provider.ProviderHandler;
import red.jackf.chesttracker.storage.ConnectionSettings;
import red.jackf.chesttracker.storage.Storage;
import red.jackf.jackfredlib.api.base.ResultHolder;
import red.jackf.jackfredlib.client.api.gps.Coordinate;
import red.jackf.whereisit.api.search.ConnectedBlocksGrabber;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils.client;
import static me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils.printerMemoryAdding;

public class MemoryUtils {
    public static MemoryBank PRINTER_MEMORY = null;

    public static void deletePrinterMemory() {
        if (PRINTER_MEMORY != null) {
            String id = PRINTER_MEMORY.getId();
            unLoad();
            Storage.delete(id);
            createPrinterMemory();
        }
    }

    public static void setup() {
        //破坏方块后清除打印机库存的该记录
        AfterPlayerDestroyBlock.EVENT.register(cbs -> {
            if (PRINTER_MEMORY != null
                    && PRINTER_MEMORY.getMetadata().getIntegritySettings().removeOnPlayerBlockBreak
            ) {
                var currentKey = ProviderHandler.getCurrentKey();
                if (currentKey != null) {
                    PRINTER_MEMORY.removeMemory(currentKey, cbs.pos());
//                    LOGGER.debug("Player Destroy Block: Removing {}@{}", cbs.pos().toShortString(), currentKey);
                }
            }
        });

        //关闭屏幕后保存
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?>) {
                ScreenEvents.remove(screen).register(screen1 -> {
                    if(printerMemoryAdding && PRINTER_MEMORY != null)
                        save((HandledScreen<?>) screen1, PRINTER_MEMORY);
                    else save((HandledScreen<?>) screen1, MemoryBank.INSTANCE);
                });
            }
        });

        //加载打印机库存
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            createPrinterMemory();
        }));
        //保存打印机库存
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            unLoad();
        });
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
                var bank = new MemoryBank(Metadata.blankWithName(coordinate.userFriendlyName() + "-printer"), new HashMap<>());
                bank.setId(s1);
                return bank;
            });
            save();
        }
    }

    public static void unLoad() {
        if (PRINTER_MEMORY != null) {
            save();
        }
        PRINTER_MEMORY = null;
    }

    public static void save() {
        Storage.save(PRINTER_MEMORY);
    }

    public static void save(HandledScreen<?> screen, MemoryBank memoryBank) {
        if (memoryBank == null || OpenInventoryPacket.key == null || Statistics.blockState == null || !LitematicaMixinMod.INVENTORY.getBooleanValue()) return;
        List<BlockPos> connected;
        if (ZxyUtils.printerMemoryAdding && client.world != null) {
            connected = ConnectedBlocksGrabber.getConnected(client.world, client.world.getBlockState(OpenInventoryPacket.pos), OpenInventoryPacket.pos);
        } else connected = null;

        List<ItemStack> items = ProviderUtils.getNonPlayerStacksAsList(screen);
        ResultHolder<MemoryBuilder.Entry> value = ResultHolder.value(MemoryBuilder.create(items)
                .inContainer(Statistics.blockState.getBlock())
                .otherPositions(connected != null ? connected.stream()
                        .filter(pos -> {
                            return connected.isEmpty() || !pos.equals(connected.get(0));
                        })
                        .toList() : List.of(OpenInventoryPacket.pos)
                )
                .toEntry(OpenInventoryPacket.key.getValue(), OpenInventoryPacket.pos)
        );
        if (memoryBank != null) {
            memoryBank.addMemory(value.get());
        }
        OpenInventoryPacket.key = null;
        OpenInventoryPacket.pos = null;
    }
}