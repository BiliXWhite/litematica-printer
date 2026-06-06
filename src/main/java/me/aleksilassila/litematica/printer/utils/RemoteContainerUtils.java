package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import me.aleksilassila.litematica.printer.network.RemoteInventoryNetwork;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.container.ContainerItemCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class RemoteContainerUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    private static final Set<BlockPos> knownContainers = new HashSet<>();
    private static List<BlockPos> containerList = new ArrayList<>();
    private static long lastScan = 0;
    private static long lastCacheClear = 0;

    private static final Map<String, ItemFetchState> fetchStates = new ConcurrentHashMap<>();
    private static final Map<BlockPos, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    private record PendingRequest(String itemId, int slot) {}

    private static class ItemFetchState {
        final String itemId;
        int scanIndex;
        boolean triedCache;
        // True while a network request is in-flight — prevents flooding the server
        // with multiple scan/get requests for the same item in a single iteration.
        boolean requestPending;

        ItemFetchState(String itemId) {
            this.itemId = itemId;
        }

        void reset() {
            triedCache = false;
            scanIndex = 0;
            requestPending = false;
        }
    }

    public static void init() {
        RemoteInventoryNetwork.setResultCallback(RemoteContainerUtils::onGetItemResult);
        RemoteInventoryNetwork.setScanResultCallback(RemoteContainerUtils::onScanResult);
    }

    // ──────── 容器扫描 ────────

    public static void scanContainerPos() {
        if (SchematicWorldHandler.getSchematicWorld() == null) return;

        knownContainers.clear();
        Level level = mc.level;
        if (level == null) return;

        List<PrinterBox> selectionBoxes = getSelectionBoxes();
        if (selectionBoxes.isEmpty()) return;

        Set<String> containerBlockIds = resolveContainerBlockIds();
        for (PrinterBox box : selectionBoxes) {
            for (BlockPos pos : box) {
                Block block = level.getBlockState(pos).getBlock();
                if (containerBlockIds.contains(BuiltInRegistries.BLOCK.getKey(block).toString())) {
                    knownContainers.add(pos.immutable());
                }
            }
        }
        containerList = new ArrayList<>(knownContainers);
    }

    private static List<PrinterBox> getSelectionBoxes() {
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) return Collections.emptyList();

        if (DataManager.getSelectionManager().getSelectionMode() == SelectionMode.NORMAL) {
            return selection.getAllSubRegionBoxes().stream()
                    .map(RemoteContainerUtils::toPrinterBox)
                    .filter(Objects::nonNull)
                    .toList();
        }

        Box box = selection.getSubRegionBox(DataManager.getSimpleArea().getName());
        PrinterBox pb = toPrinterBox(box);
        return pb != null ? Collections.singletonList(pb) : Collections.emptyList();
    }

    private static PrinterBox toPrinterBox(Box box) {
        if (box == null || box.getPos1() == null || box.getPos2() == null) return null;
        return new PrinterBox(box.getPos1(), box.getPos2());
    }

    // ──────── 核心：缓存优先取物 ────────

    public static boolean tryGetItemFromContainers(Item item) {
        long now = System.currentTimeMillis();
        if (now - lastScan > 5000) {
            scanContainerPos();
            lastScan = now;
        }

        if (knownContainers.isEmpty()) {
            scanContainerPos();
            if (knownContainers.isEmpty()) return false;
        }

        String itemId = getItemId(item);
        ItemFetchState state = fetchStates.computeIfAbsent(itemId, ItemFetchState::new);

        // If a request is already in-flight for this item, don't send another one.
        // Without this guard, every block needing the same item triggers a new
        // server request in the same iteration, flooding the server and causing lag.
        if (state.requestPending) return true;

        if (!state.triedCache) {
            state.triedCache = true;
            ContainerItemCache.SlotRef ref = ContainerItemCache.INSTANCE.findItem(itemId);
            if (ref != null) {
                pendingRequests.put(ref.pos(), new PendingRequest(itemId, ref.slot()));
                state.requestPending = true;
                RemoteInventoryNetwork.sendGetItemRequest(ref.pos(), itemId, ref.slot());
                return true;
            }
        }

        while (state.scanIndex < containerList.size()) {
            BlockPos pos = containerList.get(state.scanIndex++);
            if (!ContainerItemCache.INSTANCE.isCached(pos)) {
                state.requestPending = true;
                RemoteInventoryNetwork.sendScanContainerRequest(pos);
                return true;
            }
        }

        // 所有容器都扫描过了，还是没找到，2秒后重置状态等待下一轮扫描
        if (now - lastCacheClear > 2000) {
            ContainerItemCache.INSTANCE.clear();
            lastCacheClear = now;
        }
        state.reset();
        return false;
    }

    private static void onScanResult(
            me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload payload) {
        ContainerItemCache.INSTANCE.updateContainer(payload.getPos(), payload.getEntries());
        // Release the pending flag so the next tryGetItemFromContainers call can proceed.
        // Don't reset scanIndex — let each state continue from where it left off.
        for (ItemFetchState s : fetchStates.values()) {
            s.requestPending = false;
        }
    }

    private static void onGetItemResult(BlockPos pos, RemoteResultType result) {
        PendingRequest pending = pendingRequests.remove(pos);
        if (pending == null) return;

        if (result == RemoteResultType.SUCCESS) {
            ContainerItemCache.INSTANCE.markSlotUsed(pos, pending.slot());
            fetchStates.remove(pending.itemId());
        } else {
            ContainerItemCache.INSTANCE.invalidate(pos);
            ItemFetchState state = fetchStates.get(pending.itemId());
            if (state != null) state.reset();
        }
    }

    private static String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Set<String> resolveContainerBlockIds() {
        Set<String> ids = new HashSet<>();
        for (String id : Configs.Print.REMOTE_CONTAINER_BLOCKS.getStrings()) {
            if (id != null && !id.isEmpty()) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            ids.add("minecraft:chest");
            ids.add("minecraft:trapped_chest");
            ids.add("minecraft:barrel");
        }
        return ids;
    }

    public static void reset() {
        fetchStates.clear();
        ContainerItemCache.INSTANCE.clear();
    }
}
