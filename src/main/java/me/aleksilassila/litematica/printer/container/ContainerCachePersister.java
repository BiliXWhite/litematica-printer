package me.aleksilassila.litematica.printer.container;

import com.google.gson.Gson;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Environment(EnvType.CLIENT)
public class ContainerCachePersister {
    private static final Gson GSON = new Gson();

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> save());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> load());
    }

    static void save() {
        Map<BlockPos, List<ScanContainerResultPayload.SlotEntry>> data =
                ContainerItemCache.INSTANCE.exportContainerData();
        if (data.isEmpty()) return;

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        for (var slots : data.values()) {
            for (var se : slots) {
                String id = se.itemId();
                if (!paletteIndex.containsKey(id)) {
                    paletteIndex.put(id, palette.size());
                    palette.add(id);
                }
            }
        }

        List<CacheEntry> entries = new ArrayList<>();
        for (var e : data.entrySet()) {
            List<SlotData> slots = new ArrayList<>();
            for (var se : e.getValue())
                slots.add(new SlotData(se.slot(), paletteIndex.get(se.itemId()), se.count()));
            entries.add(new CacheEntry(e.getKey().getX(), e.getKey().getY(), e.getKey().getZ(), slots));
        }

        try {
            Files.writeString(cachePath(), GSON.toJson(new CacheFile(palette, entries)));
            Reference.LOGGER.info("[Cache] saved {} containers, {} items to {}",
                    entries.size(), palette.size(), cachePath());
        } catch (IOException e) {
            Reference.LOGGER.warn("[Cache] save failed: {}", e.getMessage());
        }
    }

    static void load() {
        Path path = cachePath();
        if (!Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            CacheFile file = GSON.fromJson(json, CacheFile.class);
            if (file == null || file.p == null || file.c == null) return;

            List<String> palette = file.p;
            for (CacheEntry e : file.c) {
                List<ScanContainerResultPayload.SlotEntry> slots = new ArrayList<>();
                for (SlotData s : e.s) {
                    String itemId = s.i >= 0 && s.i < palette.size() ? palette.get(s.i) : "minecraft:air";
                    slots.add(new ScanContainerResultPayload.SlotEntry(s.s, itemId, s.c));
                }
                ContainerItemCache.INSTANCE.importContainer(new BlockPos(e.x, e.y, e.z), slots);
            }
            Reference.LOGGER.info("[Cache] loaded {} containers, {} items from {}",
                    file.c.size(), palette.size(), path);
        } catch (IOException e) {
            Reference.LOGGER.warn("[Cache] load failed: {}", e.getMessage());
        }
    }

    private static Path cachePath() {
        return FabricLoader.getInstance().getGameDir().resolve("printer_container_cache.json");
    }

    @SuppressWarnings("unused")
    private static class CacheFile { List<String> p; List<CacheEntry> c;
        CacheFile(List<String> p, List<CacheEntry> c) { this.p = p; this.c = c; } }
    @SuppressWarnings("unused")
    private static class CacheEntry { int x, y, z; List<SlotData> s;
        CacheEntry(int x, int y, int z, List<SlotData> s) { this.x = x; this.y = y; this.z = z; this.s = s; } }
    @SuppressWarnings("unused")
    private static class SlotData { int s; int i; int c;
        SlotData(int s, int i, int c) { this.s = s; this.i = i; this.c = c; } }
}
