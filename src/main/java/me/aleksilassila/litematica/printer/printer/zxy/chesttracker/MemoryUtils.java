package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

//#if MC > 12001
//$$ import me.aleksilassila.litematica.printer.LitematicaMixinMod;
//$$ import me.aleksilassila.litematica.printer.printer.Printer;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
//$$ import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//$$ import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//$$ import net.minecraft.block.BlockState;
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen;
//$$ import net.minecraft.entity.player.PlayerInventory;
//$$ import net.minecraft.item.ItemStack;
//$$ import net.minecraft.screen.ScreenHandler;
//$$ import net.minecraft.screen.slot.Slot;
//$$ import net.minecraft.text.Text;
//$$ import net.minecraft.util.Identifier;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import red.jackf.chesttracker.api.events.AfterPlayerDestroyBlock;
//$$ import red.jackf.chesttracker.api.provider.MemoryBuilder;
//$$ import red.jackf.chesttracker.memory.MemoryBank;
//$$ import red.jackf.chesttracker.memory.metadata.Metadata;
//$$ import red.jackf.chesttracker.provider.ProviderHandler;
//$$ import red.jackf.chesttracker.storage.ConnectionSettings;
//$$ import red.jackf.chesttracker.storage.Storage;
//$$ import red.jackf.jackfredlib.api.base.ResultHolder;
//$$ import red.jackf.jackfredlib.client.api.gps.Coordinate;
//$$ import red.jackf.whereisit.api.SearchRequest;
//$$ import red.jackf.whereisit.api.search.ConnectedBlocksGrabber;
//$$
//$$ import java.util.HashMap;
//$$ import java.util.List;
//$$ import java.util.Optional;
//$$
//$$ import static me.aleksilassila.litematica.printer.LitematicaMixinMod.INVENTORY;
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket.openIng;
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.printerMemoryAdding;
//$$
//$$ public class MemoryUtils {
//$$     public static MemoryBank PRINTER_MEMORY = null;
//$$
//$$     //点击的物品
//$$     public static ItemStack itemStack = null;
//$$     //当前打开的维度
//$$     public static Identifier currentMemoryKey = null;
//$$     //远程取物返回包中的方块数据
//$$     public static BlockState blockState = null;
//$$     //箱子追踪搜索请求
//$$     public static SearchRequest request = null;
//$$
//$$     public static void deletePrinterMemory() {
//$$         if (PRINTER_MEMORY != null) {
//$$             String id = PRINTER_MEMORY.getId();
//$$             unLoad();
//$$             Storage.delete(id);
//$$             createPrinterMemory();
//$$         }
//$$         ZxyUtils.client.inGameHud.setOverlayMessage(Text.of("打印机库存已清空"), false);
//$$     }
//$$
//$$     public static void setup() {
//$$         //破坏方块后清除打印机库存的该记录
//$$         AfterPlayerDestroyBlock.EVENT.register(cbs -> {
//$$             if (PRINTER_MEMORY != null
//$$                     && PRINTER_MEMORY.getMetadata().getIntegritySettings().removeOnPlayerBlockBreak
//$$             ) {
//$$                 var currentKey = ProviderHandler.getCurrentKey();
//$$                 if (currentKey != null) {
//$$                     PRINTER_MEMORY.removeMemory(currentKey, cbs.pos());
//$$ //                    LOGGER.debug("Player Destroy Block: Removing {}@{}", cbs.pos().toShortString(), currentKey);
//$$                 }
//$$             }
//$$         });
//$$
//$$         //关闭屏幕后保存 在屏蔽掉ui的情况下 这里可能无法触发 建议在mixin中调用保存方法
//$$         ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
//$$             if (screen instanceof HandledScreen<?> sc) {
//$$                 ScreenEvents.remove(screen).register(screen1 -> {
//$$ //                    saveMemory(sc.getScreenHandler());
//$$                 });
//$$             }
//$$         });
//$$
//$$         //加载打印机库存
//$$         ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
//$$             createPrinterMemory();
//$$         }));
//$$         //保存打印机库存
//$$         ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
//$$             unLoad();
//$$         });
//$$     }
//$$     public static void saveMemory(ScreenHandler sc){
//$$         if(PRINTER_MEMORY != null && ZxyUtils.printerMemoryAdding || Printer.printerMemorySync)
//$$             save(sc , PRINTER_MEMORY);
//$$         if(MemoryBank.INSTANCE != null)
//$$             save(sc , MemoryBank.INSTANCE);
//$$         Printer.printerMemorySync = false;
//$$     }
//$$     public static void createPrinterMemory(){
//$$         Optional<Coordinate> current = Coordinate.getCurrent();
//$$         if (current.isPresent()) {
//$$             Coordinate coordinate = current.get();
//$$             String s1 = coordinate.id() + "-printer";
//$$
//$$             ConnectionSettings orCreate = ConnectionSettings.getOrCreate(s1);
//$$             String s = orCreate.memoryBankIdOverride().orElse(s1);
//$$
//$$             unLoad();
//$$             PRINTER_MEMORY = Storage.load(s).orElseGet(() -> {
//$$                 var bank = new MemoryBank(Metadata.blankWithName(coordinate.userFriendlyName() + "-printer"), new HashMap<>());
//$$                 bank.setId(s1);
//$$                 return bank;
//$$             });
//$$             save();
//$$         }
//$$     }
//$$
//$$     public static void unLoad() {
//$$         if (PRINTER_MEMORY != null) {
//$$             save();
//$$         }
//$$         PRINTER_MEMORY = null;
//$$     }
//$$
//$$     public static void save() {
//$$         Storage.save(PRINTER_MEMORY);
//$$     }
//$$
//$$     public static void save(ScreenHandler screen , MemoryBank memoryBank) {
//$$         if (memoryBank == null || OpenInventoryPacket.key == null || blockState == null || !LitematicaMixinMod.INVENTORY.getBooleanValue()) return;
//$$         List<BlockPos> connected;
//$$         if (ZxyUtils.printerMemoryAdding && ZxyUtils.client.world != null) {
//$$             connected = ConnectedBlocksGrabber.getConnected(ZxyUtils.client.world, ZxyUtils.client.world.getBlockState(OpenInventoryPacket.pos), OpenInventoryPacket.pos);
//$$         } else connected = null;
//$$         List<ItemStack> items;
//$$         if (screen !=null)
//$$             items = screen.slots.stream()
//$$                     .filter(slot -> !(slot.inventory instanceof PlayerInventory))
//$$                     .map(Slot::getStack)
//$$                     .toList();
//$$         else return;
//$$
//$$         ResultHolder<MemoryBuilder.Entry> value = ResultHolder.value(MemoryBuilder.create(items)
//$$                 .inContainer(blockState.getBlock())
//$$                 .otherPositions(connected != null ? connected.stream()
//$$                         .filter(pos -> !pos.equals(connected.get(0)))
//$$                         .toList() : List.of(OpenInventoryPacket.pos)
//$$                 )
//$$                 .toEntry(OpenInventoryPacket.key.getValue(), OpenInventoryPacket.pos)
//$$         );
//$$         if (memoryBank != null) {
//$$             memoryBank.addMemory(value.get());
//$$         }
//$$     }
//$$ }
//#endif