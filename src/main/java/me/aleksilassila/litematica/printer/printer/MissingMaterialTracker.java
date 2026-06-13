package me.aleksilassila.litematica.printer.printer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MissingMaterialTracker {
    private static final MissingMaterialTracker INSTANCE = new MissingMaterialTracker();

    public static class Entry {
        public final Item item;
        public Component displayName;
        public int lastSeenGeneration;

        public Entry(Item item, Component displayName, int generation) {
            this.item = item;
            this.displayName = displayName;
            this.lastSeenGeneration = generation;
        }

        public Entry(Item item) {
            this.item = item;
            this.displayName = null;
            this.lastSeenGeneration = 0;
        }
    }

    private final Map<Item, Entry> missingMap = new ConcurrentHashMap<>();
    private int generation = 0;

    public static MissingMaterialTracker getInstance() {
        return INSTANCE;
    }

    /**
     * 开始新的一轮迭代周期：递增代数，移除超过一代未更新的条目。
     * 条目在当前代或上一代中被记录则保留，否则视为陈旧数据被清除。
     */
    public void startCycle() {
        generation++;
        if (missingMap.isEmpty()) return;
        int threshold = generation - 1;
        missingMap.values().removeIf(e -> e.lastSeenGeneration < threshold);
    }

    public void recordMissing(Item item, Component displayName) {
        if (item == null || item == Items.AIR) return;
        missingMap.compute(item, (k, existing) -> {
            Entry entry = existing != null ? existing : new Entry(item, displayName, generation);
            entry.displayName = displayName;
            entry.lastSeenGeneration = generation;
            return entry;
        });
    }

    public List<Entry> getMissing() {
        return new ArrayList<>(missingMap.values());
    }

    public boolean hasMissing() {
        return !missingMap.isEmpty();
    }

    public int size() {
        return missingMap.size();
    }

    public int getGeneration() {
        return generation;
    }

    public void reset() {
        missingMap.clear();
    }
}
