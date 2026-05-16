package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

//#if MC >= 12001

import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.utils.ZxyUtils;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import red.jackf.chesttracker.api.memory.Memory;
import red.jackf.chesttracker.api.memory.MemoryBank;
import red.jackf.chesttracker.api.memory.MemoryKey;
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
import java.util.Map;
import java.util.Optional;

//#if MC >= 260000
//$$ import fi.dy.masa.malilib.util.data.ItemType;
//#else
import fi.dy.masa.malilib.util.ItemType;
//#endif

public class MemoryUtils {
    @NotNull
    static Minecraft client = Minecraft.getInstance();

    public static MemoryBankImpl PRINTER_MEMORY = null;
//    public static MemoryBankImpl memoryBankImpl = MemoryBankAccessImpl.INSTANCE.getLoadedInternal().get();

    //点击的物品
    public static ItemStack itemStack = null;
    //当前打开的维度
    public static Identifier currentMemoryKey = null;
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
        MessageUtils.setOverlayMessage(I18n.INVENTORY_SYNC_CLEARED.getName());
    }

    public static void setup() {
        // 破坏方块后清除打印机库存的该记录
        AfterPlayerDestroyBlock.EVENT.register(cbs -> {
            if (PRINTER_MEMORY != null
                && PRINTER_MEMORY.getMetadata().getIntegritySettings().removeOnPlayerBlockBreak
            ) {
                ProviderUtils.getPlayersCurrentKey().ifPresent(currentKey -> PRINTER_MEMORY.removeMemory(currentKey, cbs.pos()));
            }
        });

//        // 关闭屏幕后保存 在屏蔽掉ui的情况下 这里可能无法触发 建议在mixin中调用保存方法
//        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
//            if (screen instanceof AbstractContainerScreen<?> sc) {
//                ScreenEvents.remove(screen).register(screen1 -> {
//                    saveMemory(sc.getMenu());
//                });
//            }
//        });

        //加载打印机库存
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(MemoryUtils::createPrinterMemory));
        //保存打印机库存
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> unLoad());
    }

    public static void saveMemory(AbstractContainerMenu sc) {
        if (PRINTER_MEMORY != null && ZxyUtils.printerMemoryAdding || ClientPlayerTickManager.PRINT.isPrinterMemorySync())
            save(sc, PRINTER_MEMORY);
        MemoryBankAccessImpl.INSTANCE.getLoadedInternal().ifPresent(memoryBank -> save(sc, memoryBank));
        ClientPlayerTickManager.PRINT.setPrinterMemorySync(false);
    }

    public static void createPrinterMemory() {
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
            if (printerMetadata != null) {
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

    public static void save(AbstractContainerMenu screen, MemoryBank memoryBank) {
        if (memoryBank == null || OpenInventoryPacket.key == null || blockState == null || !Configs.Core.CLOUD_INVENTORY.getBooleanValue())
            return;
        List<BlockPos> connected;
        if (ZxyUtils.printerMemoryAdding && client.level != null) {
            connected = ConnectedBlocksGrabber.getConnected(client.level, client.level.getBlockState(OpenInventoryPacket.pos), OpenInventoryPacket.pos);
        } else connected = null;
        List<ItemStack> items;
        if (screen != null)
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
            memoryBank.addMemory(OpenInventoryPacket.key.identifier(), OpenInventoryPacket.pos, memory);
        }
    }

    @SuppressWarnings("deprecation")
    public static void getSelectionContainerItems(List<PrinterBox> boxes, List<Object2IntOpenHashMap<ItemType>> result) {
        if (boxes == null || result == null || boxes.isEmpty() || client.level == null) {
            return;
        }

        MemoryBankImpl bank = MemoryBankAccessImpl.INSTANCE.getLoadedInternal().orElse(null);
        if (bank == null) {
            return;
        }

        Identifier dimensionKey = client.level.dimension().identifier();
        Optional<MemoryKey> optional = bank.getKey(dimensionKey);
        if (optional.isEmpty()) {
            return;
        }
        Map<BlockPos, Memory> memories = optional.get().getMemories();

        if (memories == null || memories.isEmpty()) {
            return;
        }

        for (PrinterBox box : boxes) {
            if (box == null) {
                continue;
            }

            for (BlockPos pos : box) {
                Memory memory = memories.get(pos);
                if (memory == null) {
                    continue;
                }

                result.add(convertChestTrackerItems(memory));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static Object2IntOpenHashMap<ItemType> convertChestTrackerItems(Memory memory) {
        Object2IntOpenHashMap<ItemType> result = new Object2IntOpenHashMap<>();
        if (memory == null || memory.items() == null) {
            return result;
        }

        for (ItemStack stack : memory.items()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            // From `MaterialListUtils.getInventoryItemCounts#L174-L203`
            Item item = stack.getItem();
            if (item instanceof BlockItem blockItem &&
                blockItem.getBlock() instanceof ShulkerBoxBlock &&
                InventoryUtils.shulkerBoxHasItems(stack)
            ) {
                Object2IntOpenHashMap<ItemType> boxCounts = MaterialListUtils.getStoredItemCounts(stack);

                for (ItemType boxType : boxCounts.keySet()) {
                    result.addTo(boxType, boxCounts.getInt(boxType));
                }

                boxCounts.clear();
            } else if (item instanceof BundleItem && InventoryUtils.bundleHasItems(stack)) {
                Object2IntOpenHashMap<ItemType> bundleCounts = MaterialListUtils.getBundleItemCounts(stack);

                for (ItemType bundleType : bundleCounts.keySet()) {
                    result.addTo(bundleType, bundleCounts.getInt(bundleType));
                }

                bundleCounts.clear();
            } else {
                result.addTo(new ItemType(stack, true, false), stack.getCount());
            }
        }

        return result;
    }
}
//#endif