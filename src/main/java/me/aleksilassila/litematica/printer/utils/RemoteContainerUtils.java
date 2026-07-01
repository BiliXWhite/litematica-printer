package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.config.Configs;
import dev.blinkwhite.remoteinventory.client.ContainerItemCache;
import dev.blinkwhite.remoteinventory.client.ContainerReturnTracker;
import dev.blinkwhite.remoteinventory.client.RemoteInventoryClient;
import dev.blinkwhite.remoteinventory.enums.ResultType;
import dev.blinkwhite.remoteinventory.network.payload.RemoteExchangeResultPayload;
import dev.blinkwhite.remoteinventory.network.payload.ScanContainerResultPayload;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class RemoteContainerUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final boolean REMOTE_INVENTORY_LOADED = ModUtils.isRemoteInventoryNextLoaded();

    private static final Set<BlockPos> knownContainers = new HashSet<>();
    private static List<BlockPos> containerList = new ArrayList<>();
    private static long lastScan = 0;

    private static final Map<String, ItemFetchState> fetchStates = new ConcurrentHashMap<>();

    // exchange 期间 PrintHandler 必须跳过所有方块，否则手可能被掏空导致放错
    public static boolean hasPendingExchange() {
        return pendingExchange != null;
    }

    // 每种物品的取物进度：先查缓存，缓存 miss 后渐进扫描容器
    private static class ItemFetchState {
        final String itemId;
        int scanIndex;
        boolean triedCache;
        boolean requestPending;
        ItemFetchState(String itemId) { this.itemId = itemId; }
        void reset() { triedCache = false; scanIndex = 0; requestPending = false; }
    }

    public static void init() {
        if (!REMOTE_INVENTORY_LOADED) return;
        RemoteInventoryClient.setExchangeCallback(RemoteContainerUtils::onExchangeResult);
        RemoteInventoryClient.setScanResultCallback(RemoteContainerUtils::onScanResult);
    }

    public static void tick() {
    }

    private static String getCurrentDimension() {
        if (mc.level == null) return "";
        //#if MC >= 12006
        // MC >= 1.20.6: server sends ResourceKey#toString() format
        return mc.level.dimension().toString();
        //#else
        // MC < 1.20.6: server sends the identifier format
        //$$ return mc.level.dimension().location().toString();
        //#endif
    }

    private record PendingExchange(BlockPos takePos, String takeItemId, int takeSlot,
                                   BlockPos returnPos, String returnItemId, int returnRequested) {}
    private static PendingExchange pendingExchange = null;

    private static void onExchangeResult(BlockPos pos, ResultType takeResult,
                                          int takenCount, int returnedCount,
                                          List<RemoteExchangeResultPayload.SlotSnapshot> inventoryDelta) {
        PendingExchange pending = pendingExchange;
        pendingExchange = null;
        if (pending == null) {
            for (ItemFetchState s : fetchStates.values()) s.requestPending = false;
            return;
        }

        applyInventoryDelta(inventoryDelta);

        String dim = getCurrentDimension();

        // tracker: 全部还回则移除条目；缓存: 记录归还数量
        if (returnedCount > 0 && !pending.returnItemId().isEmpty()) {
            if (returnedCount >= pending.returnRequested())
                ContainerReturnTracker.INSTANCE.remove(new ContainerReturnTracker.ReturnEntry(
                        dim, pending.returnPos(), pending.returnItemId(), 0));
            ContainerItemCache.INSTANCE.recordReturn(dim, pending.returnPos(), pending.returnItemId(), returnedCount);
        }

        // tracker: 全部还回则移除条目；缓存: 记录取物数量
        if (takenCount > 0 && !pending.takeItemId().isEmpty()) {
            ContainerReturnTracker.INSTANCE.track(dim, pending.takePos(), pending.takeItemId());
            ContainerItemCache.INSTANCE.recordTake(dim, pending.takePos(), pending.takeSlot(), takenCount);
            if (takeResult == ResultType.SUCCESS)
                fetchStates.remove(pending.takeItemId());
        } else if (pending.takeSlot() >= 0 && !pending.takeItemId().isEmpty()
                && takenCount <= 0
                && takeResult != ResultType.SUCCESS
                && takeResult != ResultType.PARTIAL) {
            ContainerItemCache.INSTANCE.invalidate(dim, pending.takePos());
        }

        for (ItemFetchState s : fetchStates.values()) s.requestPending = false;
    }

    public static void scanContainerPos() {
        if (!REMOTE_INVENTORY_LOADED) return;
        if (SchematicWorldHandler.getSchematicWorld() == null) return;
        knownContainers.clear();
        Level level = mc.level;
        if (level == null) return;
        List<PrinterBox> selectionBoxes = getSelectionBoxes();
        if (selectionBoxes.isEmpty()) return;
        Set<String> containerBlockIds = resolveContainerBlockIds();
        for (PrinterBox box : selectionBoxes)
            for (BlockPos pos : box)
                if (containerBlockIds.contains(BuiltInRegistries.BLOCK.getKey(
                        level.getBlockState(pos).getBlock()).toString()))
                    knownContainers.add(pos.immutable());
        containerList = new ArrayList<>(knownContainers);
    }

    private static List<PrinterBox> getSelectionBoxes() {
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) return Collections.emptyList();
        if (DataManager.getSelectionManager().getSelectionMode() == SelectionMode.NORMAL)
            return selection.getAllSubRegionBoxes().stream()
                    .map(RemoteContainerUtils::toPrinterBox).filter(Objects::nonNull).toList();
        Box box = selection.getSubRegionBox(DataManager.getSimpleArea().getName());
        PrinterBox printerBox = toPrinterBox(box);
        return printerBox != null ? List.of(printerBox) : Collections.emptyList();
    }

    private static PrinterBox toPrinterBox(Box box) {
        if (box == null || box.getPos1() == null || box.getPos2() == null) return null;
        return new PrinterBox(box.getPos1(), box.getPos2());
    }

    public static boolean tryGetItemFromContainers(Item item) {
        if (!REMOTE_INVENTORY_LOADED) return false;
        long now = System.currentTimeMillis();
        if (now - lastScan > 5000) { scanContainerPos(); lastScan = now; }
        if (knownContainers.isEmpty()) { scanContainerPos(); if (knownContainers.isEmpty()) return false; }

        String itemId = getItemId(item);
        ItemFetchState state = fetchStates.computeIfAbsent(itemId, ItemFetchState::new);
        if (state.requestPending || pendingExchange != null) return true;

        if (!state.triedCache) {
            state.triedCache = true;
            String dim = getCurrentDimension();
            ContainerItemCache.SlotRef ref = ContainerItemCache.INSTANCE.findItem(itemId, dim);
            if (ref != null) {
                sendExchange(ref.pos(), itemId, ref.slot());
                state.requestPending = true;
                return true;
            }
            ContainerItemCache.INSTANCE.invalidateOldest(dim);
        }

        while (state.scanIndex < containerList.size()) {
            BlockPos pos = containerList.get(state.scanIndex++);
            if (!ContainerItemCache.INSTANCE.isCached(getCurrentDimension(), pos)) {
                state.requestPending = true;
                RemoteInventoryClient.sendScanContainerRequest(pos);
                return true;
            }
        }

        state.reset();
        return false;
    }

    private static void sendExchange(BlockPos takePos, String takeItemId, int takeSlot) {
        BlockPos returnPos = takePos;
        String returnItemId = "";
        int returnCount = 0;

        // 背包满时从 tracker 中选最优条目淘汰：pass 越小越旧，invCount 越少越不浪费
        if (mc.player != null && isInventoryFull(mc.player.getInventory())
                && Configs.Print.RETURN_TO_CONTAINER_WHEN_FULL.getBooleanValue()) {
            cleanStaleTracker();

            String currentDim = getCurrentDimension();
            ContainerReturnTracker.ReturnEntry best = null;
            int bestPass = Integer.MAX_VALUE, bestCount = Integer.MAX_VALUE;
            for (ContainerReturnTracker.ReturnEntry e : ContainerReturnTracker.INSTANCE.peekAll()) {
                if (!currentDim.equals(e.dimension())) continue;
                Item retItem = resolveItem(e.itemId());
                if (retItem == null) continue;
                int invCount = countInInventory(retItem);
                if (invCount <= 0) continue;
                if (e.pass() < bestPass || (e.pass() == bestPass && invCount < bestCount)) {
                    bestPass = e.pass(); bestCount = invCount; best = e;
                }
            }
            if (best != null) {
                returnPos = best.pos(); returnItemId = best.itemId(); returnCount = bestCount;
            }
        }

        pendingExchange = new PendingExchange(takePos, takeItemId, takeSlot,
                returnPos, returnItemId, returnCount);
        RemoteInventoryClient.sendExchange(takePos, takeItemId, takeSlot,
                returnPos, returnItemId, returnCount);
    }

    private static void cleanStaleTracker() {
        List<ContainerReturnTracker.ReturnEntry> toRemove = new ArrayList<>();
        for (ContainerReturnTracker.ReturnEntry e : ContainerReturnTracker.INSTANCE.peekAll())
            if (resolveItem(e.itemId()) == null || countInInventory(resolveItem(e.itemId())) <= 0)
                toRemove.add(e);
        for (ContainerReturnTracker.ReturnEntry e : toRemove)
            ContainerReturnTracker.INSTANCE.remove(e);
    }

    private static int countInInventory(Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static boolean isInventoryFull(net.minecraft.world.entity.player.Inventory inv) {
        for (int i = 0; i < 36; i++) if (inv.getItem(i).isEmpty()) return false;
        return true;
    }

    private static void onScanResult(
            ScanContainerResultPayload payload) {
        ContainerItemCache.INSTANCE.updateContainer(
                payload.dimension(), payload.pos(), payload.entries()
        );
        for (ItemFetchState s : fetchStates.values()) s.requestPending = false;
    }

    private static String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Item resolveItem(String itemId) {
        //#if MC >= 11903
        return BuiltInRegistries.ITEM.getOptional(
        //#else
        //$$ return net.minecraft.core.Registry.ITEM.getOptional(
        //#endif
            //#if MC >= 12105
            net.minecraft.resources.Identifier.parse(itemId)
            //#elseif MC >= 12101
            //$$ net.minecraft.resources.ResourceLocation.parse(itemId)
            //#else
            //$$ new net.minecraft.resources.ResourceLocation(itemId)
            //#endif
        ).orElse(null);
    }

    // 直接修改 mc.player.inventory 的 slot。服务端在 exchange result 里夹带了变动的 slot 快照，
    // 这样客户端不用等 vanilla inventory sync，立刻和服务器背包一致，消除 stale inventory race。
    private static void applyInventoryDelta(List<RemoteExchangeResultPayload.SlotSnapshot> delta) {
        if (mc.player == null || delta.isEmpty()) return;
        for (RemoteExchangeResultPayload.SlotSnapshot s : delta) {
            if (s.slotIndex() < 0 || s.slotIndex() >= 36) continue;
            if (s.itemId().isEmpty()) {
                mc.player.getInventory().setItem(s.slotIndex(), ItemStack.EMPTY);
            } else {
                Item item = resolveItem(s.itemId());
                if (item != null) {
                    mc.player.getInventory().setItem(s.slotIndex(),
                            new ItemStack(item, Math.max(s.count(), 1)));
                }
            }
        }
    }

    private static Set<String> resolveContainerBlockIds() {
        Set<String> ids = new HashSet<>();
        for (String id : Configs.Print.REMOTE_CONTAINER_BLOCKS.getStrings())
            if (id != null && !id.isEmpty()) ids.add(id);
        if (ids.isEmpty()) { ids.add("minecraft:chest"); ids.add("minecraft:trapped_chest"); ids.add("minecraft:barrel"); }
        return ids;
    }

    public static void reset() {
        if (!REMOTE_INVENTORY_LOADED) return;
        fetchStates.clear();
        pendingExchange = null;
        knownContainers.clear();
        containerList = new ArrayList<>();
        lastScan = 0;
        ContainerReturnTracker.INSTANCE.clear();
        ContainerItemCache.INSTANCE.clear();
    }
}