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
            for (ScanContainerResultPayload.SlotEntry e : old.entries()) {
                removeSlotRef(e.itemId(), pos, e.slot());
            }
        }

        if (entries.isEmpty()) {
            containerIndex.put(pos, new ContainerSnapshot(List.of(), System.currentTimeMillis()));
            return;
        }

        containerIndex.put(pos, new ContainerSnapshot(
                List.copyOf(entries), System.currentTimeMillis()));

        for (ScanContainerResultPayload.SlotEntry e : entries) {
            itemIndex.computeIfAbsent(e.itemId(),
                            k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new SlotRef(pos, e.slot()));
        }
    }

    public void markSlotUsed(BlockPos pos, int slot) {
        ContainerSnapshot snapshot = containerIndex.get(pos);
        if (snapshot == null) return;

        List<ScanContainerResultPayload.SlotEntry> remaining = new ArrayList<>();
        for (ScanContainerResultPayload.SlotEntry e : snapshot.entries()) {
            if (e.slot() != slot) {
                remaining.add(e);
            } else {
                removeSlotRef(e.itemId(), pos, slot);
            }
        }
        containerIndex.put(pos, new ContainerSnapshot(remaining, System.currentTimeMillis()));
    }

    public void invalidate(BlockPos pos) {
        ContainerSnapshot old = containerIndex.remove(pos);
        if (old != null) {
            for (ScanContainerResultPayload.SlotEntry e : old.entries()) {
                removeSlotRef(e.itemId(), pos, e.slot());
            }
        }
    }

    public boolean isCached(BlockPos pos) {
        return containerIndex.containsKey(pos);
    }

    public void clear() {
        itemIndex.clear();
        containerIndex.clear();
    }

    private void removeSlotRef(String itemId, BlockPos pos, int slot) {
        List<SlotRef> refs = itemIndex.get(itemId);
        if (refs == null) return;
        synchronized (refs) {
            refs.removeIf(r -> r.pos().equals(pos) && r.slot() == slot);
            if (refs.isEmpty()) {
                itemIndex.remove(itemId);
            }
        }
    }
}
