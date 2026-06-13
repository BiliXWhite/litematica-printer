package me.aleksilassila.litematica.printer.container;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ContainerCachePersister {
    private static final Gson GSON = new Gson();

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.isSameThread()) save();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isSameThread()) load();
        });
    }

    static void save() {
        List<CacheEntry> entries = new ArrayList<>();
        for (var e : ContainerItemCache.INSTANCE.exportContainerData().entrySet()) {
            List<SlotData> slots = new ArrayList<>();
            for (var se : e.getValue())
                slots.add(new SlotData(se.slot(), se.itemId(), se.count()));
            entries.add(new CacheEntry(e.getKey().getX(), e.getKey().getY(), e.getKey().getZ(), slots));
        }
        try { Files.writeString(cachePath(), GSON.toJson(entries)); } catch (IOException ignored) {}
    }

    static void load() {
        Path path = cachePath();
        if (!Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            Type listType = new TypeToken<List<CacheEntry>>() {}.getType();
            List<CacheEntry> entries = GSON.fromJson(json, listType);
            if (entries == null) return;
            for (CacheEntry e : entries) {
                List<ScanContainerResultPayload.SlotEntry> slots = new ArrayList<>();
                for (SlotData s : e.slots)
                    slots.add(new ScanContainerResultPayload.SlotEntry(s.slot, s.id, s.count));
                ContainerItemCache.INSTANCE.importContainer(new BlockPos(e.x, e.y, e.z), slots);
            }
        } catch (IOException ignored) {}
    }

    private static Path cachePath() { return Paths.get("printer_container_cache.json"); }

    @SuppressWarnings("unused")
    private static class CacheEntry { int x, y, z; List<SlotData> slots;
        CacheEntry(int x, int y, int z, List<SlotData> s) { this.x = x; this.y = y; this.z = z; this.slots = s; } }
    @SuppressWarnings("unused")
    private static class SlotData { int slot; String id; int count;
        SlotData(int s, String i, int c) { slot = s; id = i; count = c; } }
}
