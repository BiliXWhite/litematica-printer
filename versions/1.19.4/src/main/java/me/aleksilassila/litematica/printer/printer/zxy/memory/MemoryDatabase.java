package me.aleksilassila.litematica.printer.printer.zxy.memory;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.realmsclient.dto.RealmsServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.GsonHandler;
import red.jackf.chesttracker.mixins.AccessorMinecraftServer;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Environment(EnvType.CLIENT)
public class MemoryDatabase {
    private static final CompoundTag FULL_DURABILITY_TAG = new CompoundTag();
    private static @Nullable MemoryDatabase currentDatabase = null;
    private final transient String id;
    private ConcurrentMap<ResourceLocation, ConcurrentMap<BlockPos, Memory>> locations = new ConcurrentHashMap<>();
    private transient ConcurrentMap<ResourceLocation, ConcurrentMap<BlockPos, Memory>> namedLocations = new ConcurrentHashMap<>();

    private MemoryDatabase(String id) {
        this.id = id;
    }

    public static void clearCurrent() {
        if (currentDatabase != null) {
            currentDatabase.save();
            currentDatabase = null;
        }

    }

    public static @Nullable MemoryDatabase getCurrent() {
        String id = getUsableId();
        if (id == null) {
            return null;
        } else if (currentDatabase != null && currentDatabase.getId().equals(id)) {
            return currentDatabase;
        } else {
            MemoryDatabase database = new MemoryDatabase(id);
            database.load();
            currentDatabase = database;
            return database;
        }
    }

    public static @Nullable String getUsableId() {
        Minecraft mc = Minecraft.getInstance();
        String id = null;
        ClientPacketListener cpnh = mc.getConnection();
        String var10000;
        if (cpnh != null && cpnh.getConnection().isConnected()) {
            if (mc.getSingleplayerServer() != null) {
                id = "singleplayer-" + MemoryUtils.getSingleplayerName(((AccessorMinecraftServer) mc.getSingleplayerServer()).getSession());
            } else if (mc.isConnectedToRealms()) {
                RealmsServer server = MemoryUtils.getLastRealmsServer();
                if (server == null) {
                    return null;
                }

                var10000 = server.owner;
                var10000 = MemoryUtils.makeFileSafe(var10000 + "-" + server.getName());
                id = "realms-" + var10000;
            } else if (mc.getSingleplayerServer() == null && mc.getCurrentServer() != null) {
                var10000 = mc.getCurrentServer().isLan() ? "lan-" : "multiplayer-";
                id = var10000 + MemoryUtils.makeFileSafe(mc.getCurrentServer().ip);
            }
        }
        id = "printer-litematica" + "-" + id;
        return id;
    }

    public Set<ResourceLocation> getDimensions() {
        return this.locations.keySet();
    }

    public String getId() {
        return this.id;
    }

    public void save() {
        Path savePath = this.getFilePath();

        try {
            try {
                Files.createDirectory(savePath.getParent());
            } catch (FileAlreadyExistsException ignored) {
            }

            FileWriter writer = new FileWriter(savePath.toString(), StandardCharsets.UTF_8);
            GsonHandler.get().toJson(this.locations, writer);
            writer.flush();
            writer.close();

        } catch (Exception ignored) {

        }

    }

    public void load() {
        Path loadPath = this.getFilePath();
        try {
            if (Files.exists(loadPath)) {
                FileReader reader = new FileReader(loadPath.toString(), StandardCharsets.UTF_8);
                Map<ResourceLocation, Map<BlockPos, Memory>> raw = GsonHandler.get().fromJson(new JsonReader(reader), (new TypeToken<Map<ResourceLocation, Map<BlockPos, Memory>>>() {
                }).getType());
                if (raw == null) {
                    this.locations = new ConcurrentHashMap<>();
                    this.namedLocations = new ConcurrentHashMap<>();
                } else {
                    this.locations = new ConcurrentHashMap<>();
                    for (Map.Entry<ResourceLocation, Map<BlockPos, Memory>> entry : raw.entrySet()) {
                        this.locations.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
                    }
                    this.generateNamedLocations();
                }
            } else {
                this.locations = new ConcurrentHashMap<>();
                this.namedLocations = new ConcurrentHashMap<>();
            }
        } catch (Exception ignored) {
        }
    }

    private void generateNamedLocations() {
        ConcurrentMap<ResourceLocation, ConcurrentMap<BlockPos, Memory>> namedLocations = new ConcurrentHashMap<>();
        Iterator<ResourceLocation> var2 = this.locations.keySet().iterator();
        this.namedLocations = namedLocations;
    }

    public @NotNull Path getFilePath() {
        return FabricLoader.getInstance().getGameDir().resolve("printer").resolve(this.id + ".json");
    }

    public boolean positionExists(ResourceLocation worldId, BlockPos pos) {
        return this.locations.containsKey(worldId) && (this.locations.get(worldId)).containsKey(pos);
    }

    public List<ItemStack> getItems(ResourceLocation worldId) {
        if (this.locations.containsKey(worldId)) {
            Map<LightweightStack, Integer> count = new HashMap<>();
            Map<BlockPos, Memory> location = this.locations.get(worldId);
            location.forEach((pos, memory) -> {
                memory.getItems().forEach((stack) -> {
                    LightweightStack lightweightStack = new LightweightStack(stack.getItem(), stack.getTag());
                    count.merge(lightweightStack, stack.getCount(), Integer::sum);
                });
            });
            List<ItemStack> results = new ArrayList<>();
            count.forEach((lightweightStack, integer) -> {
                ItemStack stack = new ItemStack(lightweightStack.item(), integer);
                stack.setTag(lightweightStack.tag());
                results.add(stack);
            });
            return results;
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<Memory> getAllMemories(ResourceLocation worldId) {
        return (this.locations.containsKey(worldId) ? (this.locations.get(worldId)).values() : Collections.emptyList());
    }

    public Collection<Memory> getNamedMemories(ResourceLocation worldId) {
        return (this.namedLocations.containsKey(worldId) ? (this.namedLocations.get(worldId)).values() : Collections.emptyList());
    }

    public void mergeItems(ResourceLocation worldId, Memory memory, Collection<BlockPos> toRemove) {
        if (!ChestTracker.CONFIG.miscOptions.rememberNewChests && !MemoryUtils.shouldForceNextMerge()) {
            if (!this.locations.containsKey(worldId)) {
                return;
            }
            boolean exists = false;
            for (Memory existingMemory : (this.locations.get(worldId)).values()) {
                if (Objects.equals(existingMemory.getPosition(), memory.getPosition())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return;
            }
        }

        MemoryUtils.setForceNextMerge(false);
        ConcurrentMap<BlockPos, Memory> map;
        if (this.locations.containsKey(worldId)) {
            map = this.locations.get(worldId);
            map.remove(memory.getPosition());
            Objects.requireNonNull(map);
            toRemove.forEach(map::remove);
        }

        if (this.namedLocations.containsKey(worldId)) {
            map = this.namedLocations.get(worldId);
            map.remove(memory.getPosition());
            Objects.requireNonNull(map);
            toRemove.forEach(map::remove);
        }

        this.mergeItems(worldId, memory);
    }

    public void mergeItems(ResourceLocation worldId, Memory memory) {
        if (!memory.getItems().isEmpty() || memory.getTitle() != null /*|| !ChestTracker.CONFIG.miscOptions.rememberNewChests*/) {
            this.addItem(worldId, memory, this.locations);
            if (memory.getTitle() != null) {
                this.addItem(worldId, memory, this.namedLocations);
            }
        }

    }

    private void addItem(ResourceLocation worldId, Memory memory, ConcurrentMap<ResourceLocation, ConcurrentMap<BlockPos, Memory>> map) {
        ConcurrentMap<BlockPos, Memory> memoryMap = map.computeIfAbsent(worldId, (identifier) -> new ConcurrentHashMap<>());
        memoryMap.put(memory.getPosition(), memory);
    }

    public void removePos(ResourceLocation worldId, BlockPos pos) {
        Map<BlockPos, Memory> location = this.locations.get(worldId);
        if (location != null) {
            location.remove(pos);
        }

        Map<BlockPos, Memory> namedLocation = this.namedLocations.get(worldId);
        if (namedLocation != null) {
            namedLocation.remove(pos);
        }

    }

    public List<Memory> findItems(ItemStack toFind, ResourceLocation worldId) {
        List<Memory> found = new ArrayList<>();
        Map<BlockPos, Memory> location = this.locations.get(worldId);
        LocalPlayer playerEntity = Minecraft.getInstance().player;
        if (location != null && playerEntity != null) {
            Iterator<Map.Entry<BlockPos, Memory>> var6 = location.entrySet().iterator();

            while (true) {
                Map.Entry<BlockPos, Memory> entry;
                do {
                    while (true) {
                        do {
                            do {
                                if (!var6.hasNext()) {
                                    return found;
                                }
                                entry = var6.next();
                            } while (entry.getKey() == null);
                        } while ((entry.getValue()).getItems().stream().noneMatch((candidate) -> MemoryUtils.areStacksEquivalent(toFind, candidate, toFind.getTag() == null || toFind.getTag().equals(FULL_DURABILITY_TAG))));
                        break;
                    }
                } while ((entry.getValue()).getPosition() == null);
                found.add(entry.getValue());
            }
        } else {
            return found;
        }
    }

    public void clearDimension(ResourceLocation currentWorldId) {
        this.locations.remove(currentWorldId);
        this.namedLocations.remove(currentWorldId);
    }

    static {
        FULL_DURABILITY_TAG.putInt("Damage", 0);
    }
}