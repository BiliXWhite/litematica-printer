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
import me.aleksilassila.litematica.printer.container.ContainerReturnTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private static final Map<String, ItemFetchState> fetchStates = new ConcurrentHashMap<>();
    private static int exchangeCooldown = 0;

    public static boolean hasPendingExchange() {
        return pendingExchange != null || exchangeCooldown > 0;
    }

    private static class ItemFetchState {
        final String itemId;
        int scanIndex;
        boolean triedCache;
        boolean requestPending;
        ItemFetchState(String itemId) { this.itemId = itemId; }
        void reset() { triedCache = false; scanIndex = 0; requestPending = false; }
    }

    public static void init() {
        //#if MC >= 12005
        RemoteInventoryNetwork.setExchangeCallback(RemoteContainerUtils::onExchangeResult);
        //#else
        //$$ RemoteInventoryNetwork.setResultCallback(RemoteContainerUtils::onGetItemResultLegacy);
        //#endif
        RemoteInventoryNetwork.setScanResultCallback(RemoteContainerUtils::onScanResult);
    }

    public static void tick() {
        if (exchangeCooldown > 0) exchangeCooldown--;
    }

    private record PendingExchange(BlockPos takePos, String takeItemId, int takeSlot,
                                   BlockPos returnPos, String returnItemId, int returnRequested) {}
    private static PendingExchange pendingExchange = null;

    //#if MC >= 12005
    private static void onExchangeResult(BlockPos pos, RemoteResultType takeResult,
                                          int takenCount, int returnedCount) {
        PendingExchange pending = pendingExchange;
        pendingExchange = null;
        if (pending == null) {
            for (ItemFetchState s : fetchStates.values()) s.requestPending = false;
            return;
        }

        if (returnedCount > 0 && !pending.returnItemId().isEmpty()) {
            if (returnedCount >= pending.returnRequested())
                ContainerReturnTracker.INSTANCE.remove(new ContainerReturnTracker.ReturnEntry(
                        pending.returnPos(), pending.returnItemId(), 0));
            ContainerItemCache.INSTANCE.recordReturn(pending.returnPos(), pending.returnItemId(), returnedCount);
        }

        if (takenCount > 0 && !pending.takeItemId().isEmpty()) {
            ContainerReturnTracker.INSTANCE.track(pending.takePos(), pending.takeItemId());
            ContainerItemCache.INSTANCE.recordTake(pending.takePos(), pending.takeSlot(), takenCount);
            if (takeResult == RemoteResultType.SUCCESS)
                fetchStates.remove(pending.takeItemId());
        } else if (pending.takeSlot() >= 0 && !pending.takeItemId().isEmpty()
                && takenCount <= 0
                && takeResult != RemoteResultType.SUCCESS
                && takeResult != RemoteResultType.PARTIAL) {
            ContainerItemCache.INSTANCE.invalidate(pending.takePos());
        }

        if (pending != null) exchangeCooldown = 2;
        for (ItemFetchState s : fetchStates.values()) s.requestPending = false;
    }
    //#else
    //$$ private static void onGetItemResultLegacy(BlockPos pos, RemoteResultType result) {}
    //#endif

    public static void scanContainerPos() {
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
        //#if MC < 12005
        //$$ return false;
        //#else
        long now = System.currentTimeMillis();
        if (now - lastScan > 5000) { scanContainerPos(); lastScan = now; }
        if (knownContainers.isEmpty()) { scanContainerPos(); if (knownContainers.isEmpty()) return false; }

        String itemId = getItemId(item);
        ItemFetchState state = fetchStates.computeIfAbsent(itemId, ItemFetchState::new);
        if (state.requestPending || pendingExchange != null || exchangeCooldown > 0) return true;

        if (!state.triedCache) {
            state.triedCache = true;
            ContainerItemCache.SlotRef ref = ContainerItemCache.INSTANCE.findItem(itemId);
            if (ref != null) {
                sendExchange(ref.pos(), itemId, ref.slot());
                state.requestPending = true;
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

        state.reset();
        return false;
        //#endif
    }

    private static void sendExchange(BlockPos takePos, String takeItemId, int takeSlot) {
        BlockPos returnPos = takePos;
        String returnItemId = "";
        int returnCount = 0;

        if (mc.player != null && isInventoryFull(mc.player.getInventory())
                && Configs.Print.RETURN_TO_CONTAINER_WHEN_FULL.getBooleanValue()) {
            cleanStaleTracker();

            ContainerReturnTracker.ReturnEntry best = null;
            int bestPass = Integer.MAX_VALUE, bestCount = Integer.MAX_VALUE;
            for (ContainerReturnTracker.ReturnEntry e : ContainerReturnTracker.INSTANCE.peekAll()) {
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
        exchangeCooldown = 3;
        RemoteInventoryNetwork.sendExchange(takePos, takeItemId, takeSlot,
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
            me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload payload) {
        ContainerItemCache.INSTANCE.updateContainer(payload.getPos(), payload.getEntries());
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

    private static Set<String> resolveContainerBlockIds() {
        Set<String> ids = new HashSet<>();
        for (String id : Configs.Print.REMOTE_CONTAINER_BLOCKS.getStrings())
            if (id != null && !id.isEmpty()) ids.add(id);
        if (ids.isEmpty()) { ids.add("minecraft:chest"); ids.add("minecraft:trapped_chest"); ids.add("minecraft:barrel"); }
        return ids;
    }

    public static void reset() {
        fetchStates.clear();
        ContainerItemCache.INSTANCE.clear();
    }
}
