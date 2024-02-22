//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.printer.zxy.memory;

//#if MC < 12002
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Environment(EnvType.CLIENT)
public class MemoryDatabase {
    private static final NbtCompound FULL_DURABILITY_TAG = new NbtCompound();
    private static @Nullable MemoryDatabase currentDatabase = null;
    private final transient String id;
    private ConcurrentMap<Identifier, ConcurrentMap<BlockPos, Memory>> locations = new ConcurrentHashMap();
    private transient ConcurrentMap<Identifier, ConcurrentMap<BlockPos, Memory>> namedLocations = new ConcurrentHashMap();

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
        MinecraftClient mc = MinecraftClient.getInstance();
        String id = null;
        String print = null;
        ClientPlayNetworkHandler cpnh = mc.getNetworkHandler();
        String var10000;
        if (cpnh != null && cpnh.getConnection() != null && cpnh.getConnection().isOpen()) {
            if (mc.getServer() != null) {
                id = "singleplayer-" + MemoryUtils.getSingleplayerName(((AccessorMinecraftServer)mc.getServer()).getSession());
            } else if (mc.isConnectedToRealms()) {
                RealmsServer server = MemoryUtils.getLastRealmsServer();
                if (server == null) {
                    return null;
                }

                var10000 = server.owner;
                var10000 = MemoryUtils.makeFileSafe(var10000 + "-" + server.getName());
                id = "realms-" + var10000;
            } else if (mc.getServer() == null && mc.getCurrentServerEntry() != null) {
                var10000 = mc.getCurrentServerEntry().isLocal() ? "lan-" : "multiplayer-";
                id = var10000 + MemoryUtils.makeFileSafe(mc.getCurrentServerEntry().address);
            }
        }
        id = "printer-litematica" + "-" + id;
        return id;
    }

    public Set<Identifier> getDimensions() {
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
            } catch (FileAlreadyExistsException var3) {
            }

            FileWriter writer = new FileWriter(savePath.toString(), StandardCharsets.UTF_8);
            GsonHandler.get().toJson(this.locations, writer);
            writer.flush();
            writer.close();

        } catch (Exception var4) {

        }

    }

    public void load() {
        Path loadPath = this.getFilePath();

        try {
            if (Files.exists(loadPath, new LinkOption[0])) {
                FileReader reader = new FileReader(loadPath.toString(), StandardCharsets.UTF_8);
                Map<Identifier, Map<BlockPos, Memory>> raw = (Map)GsonHandler.get().fromJson(new JsonReader(reader), (new TypeToken<Map<Identifier, Map<BlockPos, Memory>>>() {
                }).getType());
                if (raw == null) {
                    this.locations = new ConcurrentHashMap();
                    this.namedLocations = new ConcurrentHashMap();
                } else {
                    this.locations = new ConcurrentHashMap();
                    Iterator var4 = raw.entrySet().iterator();

                    while(var4.hasNext()) {
                        Map.Entry<Identifier, Map<BlockPos, Memory>> entry = (Map.Entry)var4.next();
                        this.locations.put((Identifier)entry.getKey(), new ConcurrentHashMap((Map)entry.getValue()));
                    }

                    this.generateNamedLocations();
                }
            } else {
                this.locations = new ConcurrentHashMap();
                this.namedLocations = new ConcurrentHashMap();
            }
        } catch (Exception var6) {

        }

    }

    private void generateNamedLocations() {
        ConcurrentMap<Identifier, ConcurrentMap<BlockPos, Memory>> namedLocations = new ConcurrentHashMap();
        Iterator var2 = this.locations.keySet().iterator();

//        while(var2.hasNext()) {
//            Identifier worldId = (Identifier)var2.next();
//            ConcurrentMap<BlockPos, Memory> newMap = (ConcurrentMap)namedLocations.computeIfAbsent(worldId, (id) -> {
//                return new ConcurrentHashMap();
//            });
//            ((ConcurrentMap)this.locations.get(worldId)).forEach((pos, memory) -> {
//                if (memory.getTitle() != null) {
//                    newMap.put(pos, memory);
//                }
//
//            });
//        }

        this.namedLocations = namedLocations;
    }

    public @NotNull Path getFilePath() {
        return FabricLoader.getInstance().getGameDir().resolve("printer").resolve(this.id + ".json");
    }

    public boolean positionExists(Identifier worldId, BlockPos pos) {
        return this.locations.containsKey(worldId) && ((ConcurrentMap)this.locations.get(worldId)).containsKey(pos);
    }

    public List<ItemStack> getItems(Identifier worldId) {
        if (this.locations.containsKey(worldId)) {
            Map<LightweightStack, Integer> count = new HashMap();
            Map<BlockPos, Memory> location = (Map)this.locations.get(worldId);
            location.forEach((pos, memory) -> {
                memory.getItems().forEach((stack) -> {
                    LightweightStack lightweightStack = new LightweightStack(stack.getItem(), stack.getNbt());
                    count.merge(lightweightStack, stack.getCount(), Integer::sum);
                });
            });
            List<ItemStack> results = new ArrayList();
            count.forEach((lightweightStack, integer) -> {
                ItemStack stack = new ItemStack(lightweightStack.getItem(), integer);
                stack.setNbt(lightweightStack.getTag());
                results.add(stack);
            });
            return results;
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<Memory> getAllMemories(Identifier worldId) {
        return (Collection)(this.locations.containsKey(worldId) ? ((ConcurrentMap)this.locations.get(worldId)).values() : Collections.emptyList());
    }

    public Collection<Memory> getNamedMemories(Identifier worldId) {
        return (Collection)(this.namedLocations.containsKey(worldId) ? ((ConcurrentMap)this.namedLocations.get(worldId)).values() : Collections.emptyList());
    }

    public void mergeItems(Identifier worldId, Memory memory, Collection<BlockPos> toRemove) {
        if (!ChestTracker.CONFIG.miscOptions.rememberNewChests && !MemoryUtils.shouldForceNextMerge()) {
            if (!this.locations.containsKey(worldId)) {
                return;
            }

            boolean exists = false;
            Iterator var5 = ((ConcurrentMap)this.locations.get(worldId)).values().iterator();

            while(var5.hasNext()) {
                Memory existingMemory = (Memory)var5.next();
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
        ConcurrentMap map;
        if (this.locations.containsKey(worldId)) {
            map = (ConcurrentMap)this.locations.get(worldId);
            map.remove(memory.getPosition());
            Objects.requireNonNull(map);
            toRemove.forEach(map::remove);
        }

        if (this.namedLocations.containsKey(worldId)) {
            map = (ConcurrentMap)this.namedLocations.get(worldId);
            map.remove(memory.getPosition());
            Objects.requireNonNull(map);
            toRemove.forEach(map::remove);
        }

        this.mergeItems(worldId, memory);
    }

    public void mergeItems(Identifier worldId, Memory memory) {
        if (memory.getItems().size() > 0 || memory.getTitle() != null /*|| !ChestTracker.CONFIG.miscOptions.rememberNewChests*/) {
            this.addItem(worldId, memory, this.locations);
            if (memory.getTitle() != null) {
                this.addItem(worldId, memory, this.namedLocations);
            }
        }

    }

    private void addItem(Identifier worldId, Memory memory, ConcurrentMap<Identifier, ConcurrentMap<BlockPos, Memory>> map) {
        ConcurrentMap<BlockPos, Memory> memoryMap = (ConcurrentMap)map.computeIfAbsent(worldId, (identifier) -> {
            return new ConcurrentHashMap();
        });
        memoryMap.put(memory.getPosition(), memory);
    }

    public void removePos(Identifier worldId, BlockPos pos) {
        Map<BlockPos, Memory> location = (Map)this.locations.get(worldId);
        if (location != null) {
            location.remove(pos);
        }

        Map<BlockPos, Memory> namedLocation = (Map)this.namedLocations.get(worldId);
        if (namedLocation != null) {
            namedLocation.remove(pos);
        }

    }

    public List<Memory> findItems(ItemStack toFind, Identifier worldId) {
        List<Memory> found = new ArrayList();
        Map<BlockPos, Memory> location = (Map)this.locations.get(worldId);
        ClientPlayerEntity playerEntity = MinecraftClient.getInstance().player;
        if (location != null && playerEntity != null) {
            Iterator var6 = location.entrySet().iterator();

            while(true) {
                Map.Entry entry;
                do {
                    while(true) {
                        do {
                            do {
                                if (!var6.hasNext()) {
                                    return found;
                                }

                                entry = (Map.Entry)var6.next();
                            } while(entry.getKey() == null);
                        } while(!((Memory)entry.getValue()).getItems().stream().anyMatch((candidate) -> {
                            return MemoryUtils.areStacksEquivalent(toFind, candidate, toFind.getNbt() == null || toFind.getNbt().equals(FULL_DURABILITY_TAG));
                        }));
                        break;
//                        if (MemoryUtils.checkExistsInWorld((Memory)entry.getValue())) {
//                            break;
//                        }

//                        MemoryDatabase database = getCurrent();
//                        if (database != null) {
//                            database.removePos(worldId, (BlockPos)entry.getKey());
//                        }
                    }
                } while(((Memory)entry.getValue()).getPosition() == null);
                found.add((Memory)entry.getValue());
            }
        } else {
            return found;
        }
    }

    public void clearDimension(Identifier currentWorldId) {
        this.locations.remove(currentWorldId);
        this.namedLocations.remove(currentWorldId);
    }

    static {
        FULL_DURABILITY_TAG.putInt("Damage", 0);
    }
}
//#endif