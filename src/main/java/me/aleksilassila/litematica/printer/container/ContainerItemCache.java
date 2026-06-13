package me.aleksilassila.litematica.printer.container;

import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ContainerItemCache {
    public static final ContainerItemCache INSTANCE = new ContainerItemCache();

    private final Map<String, List<SlotRef>> itemIndex = new ConcurrentHashMap<>();
    private final Map<BlockPos, ContainerSnapshot> containerIndex = new ConcurrentHashMap<>();

    public record SlotRef(BlockPos pos, int slot) {}
    public record ContainerSnapshot(List<ScanContainerResultPayload.SlotEntry> entries, long timestamp) {}

    private ContainerItemCache() {}

    public SlotRef findItem(String itemId) {
        List<SlotRef> refs = itemIndex.get(itemId);
        if (refs == null || refs.isEmpty()) return null;
        synchronized (refs) {
            if (refs.isEmpty()) return null;
            return refs.remove(0);
        }
    }

    public void updateContainer(BlockPos pos, List<ScanContainerResultPayload.SlotEntry> entries) {
        ContainerSnapshot old = containerIndex.remove(pos);
        if (old != null) {
            for (ScanContainerResultPayload.SlotEntry e : old.entries())
                removeSlotRef(e.itemId(), pos, e.slot());
        }
        if (entries.isEmpty()) {
            containerIndex.put(pos, new ContainerSnapshot(List.of(), 0));
            return;
        }
        containerIndex.put(pos, new ContainerSnapshot(List.copyOf(entries), 0));
        for (ScanContainerResultPayload.SlotEntry e : entries)
            itemIndex.computeIfAbsent(e.itemId(),
                    k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new SlotRef(pos, e.slot()));
    }

    public void invalidate(BlockPos pos) {
        ContainerSnapshot old = containerIndex.remove(pos);
        if (old != null)
            for (ScanContainerResultPayload.SlotEntry e : old.entries())
                removeSlotRef(e.itemId(), pos, e.slot());
    }

    public boolean isCached(BlockPos pos) { return containerIndex.containsKey(pos); }
    public int getContainerCount() { return containerIndex.size(); }

    public void clear() {
        itemIndex.clear();
        containerIndex.clear();
    }

    public void recordTake(BlockPos pos, int slot, int takenCount) {
        ContainerSnapshot snapshot = containerIndex.get(pos);
        if (snapshot == null) return;

        List<ScanContainerResultPayload.SlotEntry> updated = new ArrayList<>();
        String itemId = null;
        for (ScanContainerResultPayload.SlotEntry e : snapshot.entries()) {
            if (e.slot() == slot) {
                itemId = e.itemId();
                int remaining = e.count() - takenCount;
                if (remaining > 0)
                    updated.add(new ScanContainerResultPayload.SlotEntry(slot, e.itemId(), remaining));
            } else {
                updated.add(e);
            }
        }
        containerIndex.put(pos, new ContainerSnapshot(updated, 0));
        if (itemId != null) {
            removeSlotRef(itemId, pos, slot);
            if (takenCount < getOriginalCount(snapshot, slot))
                itemIndex.computeIfAbsent(itemId,
                        k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(new SlotRef(pos, slot));
        }
    }

    public void recordReturn(BlockPos pos, String itemId, int returnedCount) {
        ContainerSnapshot snapshot = containerIndex.get(pos);
        if (snapshot == null) { invalidate(pos); return; }

        List<ScanContainerResultPayload.SlotEntry> updated = new ArrayList<>(snapshot.entries());
        boolean merged = false;
        for (int i = 0; i < updated.size(); i++) {
            ScanContainerResultPayload.SlotEntry e = updated.get(i);
            if (e.itemId().equals(itemId)) {
                updated.set(i, new ScanContainerResultPayload.SlotEntry(e.slot(), itemId, e.count() + returnedCount));
                merged = true;
                break;
            }
        }
        if (!merged) { invalidate(pos); return; }

        containerIndex.put(pos, new ContainerSnapshot(updated, 0));
        itemIndex.computeIfAbsent(itemId,
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new SlotRef(pos, findSlot(updated, itemId)));
    }

    private static int getOriginalCount(ContainerSnapshot snap, int slot) {
        for (ScanContainerResultPayload.SlotEntry e : snap.entries())
            if (e.slot() == slot) return e.count();
        return 0;
    }

    private static int findSlot(List<ScanContainerResultPayload.SlotEntry> entries, String itemId) {
        for (ScanContainerResultPayload.SlotEntry e : entries)
            if (e.itemId().equals(itemId)) return e.slot();
        return -1;
    }

    private void removeSlotRef(String itemId, BlockPos pos, int slot) {
        List<SlotRef> refs = itemIndex.get(itemId);
        if (refs == null) return;
        synchronized (refs) {
            refs.removeIf(r -> r.pos().equals(pos) && r.slot() == slot);
            if (refs.isEmpty()) itemIndex.remove(itemId);
        }
    }

    public void importContainer(BlockPos pos, List<ScanContainerResultPayload.SlotEntry> entries) {
        ContainerSnapshot old = containerIndex.remove(pos);
        if (old != null)
            for (ScanContainerResultPayload.SlotEntry e : old.entries())
                removeSlotRef(e.itemId(), pos, e.slot());
        if (entries.isEmpty()) {
            containerIndex.put(pos, new ContainerSnapshot(List.of(), 0));
            return;
        }
        containerIndex.put(pos, new ContainerSnapshot(List.copyOf(entries), 0));
        for (ScanContainerResultPayload.SlotEntry e : entries)
            itemIndex.computeIfAbsent(e.itemId(),
                    k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new SlotRef(pos, e.slot()));
    }

    public Map<BlockPos, List<ScanContainerResultPayload.SlotEntry>> exportContainerData() {
        Map<BlockPos, List<ScanContainerResultPayload.SlotEntry>> data = new HashMap<>();
        for (Map.Entry<BlockPos, ContainerSnapshot> e : containerIndex.entrySet())
            data.put(e.getKey(), new ArrayList<>(e.getValue().entries()));
        return data;
    }
}
