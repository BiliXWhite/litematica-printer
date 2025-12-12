package me.aleksilassila.litematica.printer.printer.zxy.memory;

import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PreprocessUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.compat.AppliedEnergisticsHandler;
import red.jackf.chesttracker.compat.ExpandedStorageHandler;

import java.util.*;
import java.util.stream.Collectors;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.syncPrinterInventory;
import static red.jackf.chesttracker.ChestTracker.id;

import com.mojang.realmsclient.dto.RealmsServer;

@Environment(EnvType.CLIENT)
public abstract class MemoryUtils {
    public static final ResourceLocation ENDER_CHEST_ID = ModLoadStatus.isLoadChestTrackerLoaded() ? id("ender_chest") : null;
    @Nullable
    private static BlockPos latestPos = null;
    @Nullable
    private static RealmsServer lastRealmsServer = null;

    private static List<Memory> currentlyCheckedMemories = new ArrayList<>();
    private static int currentlyCheckedIndex = 0;
    private static ResourceLocation currentlyCheckedWorldId = null;

    private static boolean ignoreNextMerge = false;
    private static boolean forceNextMerge = false;
    private static boolean wasEnderchest;

    private static boolean expandedStorageFailed = false;

    public static <T extends AbstractContainerMenu> void handleItemsFromScreen(@NotNull AbstractContainerMenu screen) {
        Minecraft mc = Minecraft.getInstance();
        {
//            System.out.println("===============================2");
            MemoryDatabase database = MemoryDatabase.getCurrent();
            BlockPos latestPos = MemoryUtils.getLatestPos();
            if (key == null) key = mc.level.dimension();
            if (pos != null) latestPos = pos;
            if (database != null && latestPos != null && key != null) {
                List<ItemStack> stacks = condenseItems(screen.slots.stream().filter(MemoryUtils::isValidSlot).map(Slot::getItem).collect(Collectors.toList()));
//                System.out.println(stacks);
                BlockState state = mc.level.getBlockState(latestPos);
                if (state.getBlock() == Blocks.ENDER_CHEST) {
                    database.mergeItems(MemoryUtils.ENDER_CHEST_ID, Memory.of(BlockPos.ZERO, stacks, null, null), Collections.emptyList());
                } else {
                    getTitleFromScreen(screen, mc.level.getBlockEntity(latestPos));
                    Collection<BlockPos> connected = getConnected(mc.level, latestPos);
//                    System.out.println("print-Save" + key.getValue() + latestPos);
                    database.mergeItems(key.location(), Memory.of(latestPos, stacks, null, connected.size() > 0 ? getAveragePos(latestPos, connected) : null), connected);
                    MemoryDatabase.getCurrent().mergeItems(key.location(), Memory.of(latestPos, stacks, null, connected.size() > 0 ? getAveragePos(latestPos, connected) : null), connected);
                    red.jackf.chesttracker.memory.MemoryUtils.setLatestPos(null);
                }
            }

        }


        if (mc.screen == null) {
            //        System.out.println("========================1");
            checkValidCycle(mc.level);
            red.jackf.chesttracker.memory.MemoryDatabase database = red.jackf.chesttracker.memory.MemoryDatabase.getCurrent();
            BlockPos latestPos1 = red.jackf.chesttracker.memory.MemoryUtils.getLatestPos();
            if (pos != null) latestPos1 = pos;
            if (latestPos1 == null) return;
            BlockState state = mc.level.getBlockState(latestPos1);
            if (key == null) {
                key = mc.level.dimension();
                Block block = state.getBlock();
//                System.out.println(state);
                boolean k = true;
                for (String string : InitHandler.INVENTORY_LIST.getStrings()) {
                    if (PreprocessUtils.getRegistries().BLOCK.getKey(block).toString().contains(string)) {
                        k = false;
                        break;
                    }
                }
                if (k) return;
            }

//                System.out.println("latestPos "+latestPos +"   "+ key);
            if (database != null && latestPos1 != null && key != null) {
                List<ItemStack> stacks = condenseItems(screen.slots.stream().filter(MemoryUtils::isValidSlot).map(Slot::getItem).collect(Collectors.toList()));
                if (state.getBlock() == Blocks.ENDER_CHEST) {
                    database.mergeItems(red.jackf.chesttracker.memory.MemoryUtils.ENDER_CHEST_ID, red.jackf.chesttracker.memory.Memory.of(BlockPos.ZERO, stacks, null, null), Collections.emptyList());
                } else {
                    Component title = getTitleFromScreen(screen, mc.level.getBlockEntity(latestPos1));
                    Collection<BlockPos> connected = getConnected(mc.level, latestPos1);
//                    System.out.println(stacks);
//                    System.out.println("Save" + key.getValue() + latestPos);
                    database.mergeItems(key.location(), red.jackf.chesttracker.memory.Memory.of(latestPos1, stacks, title, connected.size() > 0 ? getAveragePos(latestPos1, connected) : null), connected);
                }
            }
            if (ChestTracker.CONFIG.miscOptions.printGuiClassNames)
                ChestTracker.sendDebugMessage(Component.nullToEmpty(screen.getClass().getSimpleName()));
        }

        key = null;
        pos = null;
        red.jackf.chesttracker.memory.MemoryUtils.setLatestPos(null);
        MemoryUtils.latestPos = null;
        syncPrinterInventory = false;
    }

    public static BlockPos getMemoryPos() {
        return red.jackf.chesttracker.memory.MemoryUtils.getLatestPos();
    }

    public static void ignoreNextMerge() {
        ignoreNextMerge = true;
    }

    public static void setForceNextMerge(boolean forceNextMerge) {
        MemoryUtils.forceNextMerge = forceNextMerge;
    }

    public static boolean shouldForceNextMerge() {
        return forceNextMerge;
    }

    public static boolean isValidSlot(Slot slot) {
        try {
            return !(slot.container instanceof Inventory)
                    && !AppliedEnergisticsHandler.isAE2Slot(slot)
                    && slot.hasItem();
        } catch (Throwable ex) {
            return false;
        }
    }

    public static List<ItemStack> condenseItems(List<ItemStack> list) {
        List<ItemStack> stacks = new ArrayList<>();
        list.forEach(newStack -> {
            boolean exists = false;
            for (ItemStack oldStack : stacks) {
                if (areStacksEquivalent(newStack, oldStack, false)) {
                    oldStack.setCount(oldStack.getCount() + newStack.getCount());
                    exists = true;
                }
            }
            if (!exists) stacks.add(newStack);
        });
        return stacks;
    }

    public static Vec3 getAveragePos(BlockPos basePos, Collection<BlockPos> connected) {
        Vec3 base = Vec3.atLowerCornerOf(basePos);
        for (BlockPos pos : connected) {
            base = base.add(Vec3.atLowerCornerOf(pos));
        }
        return base.scale(1f / (1 + connected.size())).subtract(Vec3.atLowerCornerOf(basePos));
    }

    public static Collection<BlockPos> getConnected(@NotNull Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                boolean left = state.getValue(ChestBlock.TYPE) == ChestType.LEFT;
                switch (state.getValue(ChestBlock.FACING)) {
                    case NORTH:
                        return Collections.singleton(pos.offset(left ? 1 : -1, 0, 0));
                    case SOUTH:
                        return Collections.singleton(pos.offset(left ? -1 : 1, 0, 0));
                    case WEST:
                        return Collections.singleton(pos.offset(0, 0, left ? -1 : 1));
                    case EAST:
                        return Collections.singleton(pos.offset(0, 0, left ? 1 : -1));
                }
            }
        } else if (!expandedStorageFailed && FabricLoader.getInstance().isModLoaded("expandedstorage")) {
            try {
                Collection<BlockPos> result = ExpandedStorageHandler.check(state, world, pos);
                if (!result.isEmpty()) return result;
            } catch (Error ex) {

                expandedStorageFailed = true;
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    public static Component getTitleFromScreen(AbstractContainerMenu screen, @Nullable BlockEntity blockEntity) {
        Component title = screen.getSlot(0).getItem().getHoverName();
//        if (title.getContent() instanceof TranslatableTextContent) { // Likely the default.
        /*if (title instanceof TranslatableText) { // Likely the default.
            return null;
        } else */
        if (blockEntity instanceof MenuProvider) {
            return title;
        } else {
            return null;
        }
    }

    public static <T extends AbstractContainerMenu> boolean validScreenToTrack(AbstractContainerScreen<T> screen) {
        return !(screen instanceof EffectRenderingInventoryScreen) && screen != null;
    }

    @Nullable
    public static BlockPos getLatestPos() {
        return latestPos;
    }

    public static void setLatestPos(@Nullable BlockPos latestPos) {
        MemoryUtils.latestPos = latestPos != null ? latestPos.immutable() : null;
    }

    public static String getSingleplayerName(LevelStorageSource.LevelStorageAccess session) {
        //return makeFileSafe(session.getDirectoryName());
        return session.getLevelId();
    }

    public static String makeFileSafe(String name) {
        String filteredString;
        filteredString = name.replaceAll("(?U)[^\\p{Alnum} _\\-{}#'@~()]", "_");
        if (filteredString.length() > 180) {
            return filteredString.substring(0, 180);
        } else {
            return filteredString;
        }
    }

    public static boolean areStacksEquivalent(@NotNull ItemStack stack1, @NotNull ItemStack stack2, boolean ignoreNbt) {
//        System.out.println("item1 " + stack1);
//        System.out.println("item2 " + stack2);
        return stack1.getItem() == stack2.getItem()
                && (ignoreNbt
                || (!stack1.hasTag() && !stack2.hasTag())
                || Objects.equals(stack1.getTag(), stack2.getTag())
        )
                ||
                fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack2, -1).stream().anyMatch((candidate) -> {
                    return MemoryUtils.areStacksEquivalent(stack1, candidate, stack1.getTag() == null);
                });
    }

    @Nullable
    public static RealmsServer getLastRealmsServer() {
        return lastRealmsServer;
    }

    public static void setLastRealmsServer(@Nullable RealmsServer lastRealmsServer) {
        MemoryUtils.lastRealmsServer = lastRealmsServer;
    }

    public static boolean checkExistsInWorld(Memory memory) {
        return checkExistsInWorld(memory, Minecraft.getInstance().level);
    }

    public static boolean checkExistsInWorld(Memory memory, ClientLevel world) {
        BlockPos pos = memory.getPosition();
        if (world != null && pos != null) {
            LevelChunk chunk = world.getChunkAt(pos);
            return chunk instanceof EmptyLevelChunk || isValidInventoryHolder(chunk.getBlockState(pos).getBlock(), world, pos);
        }
        return true;
    }

    public static boolean isValidInventoryHolder(Block block, Level world, BlockPos pos) {
        //if (FabricLoader.getInstance().isModLoaded("universalcomponents")) {
        //return UniversalComponentsHandler.isValidInventoryHolder(block, world, pos);
        //} else {
        return block instanceof EntityBlock || block instanceof WorldlyContainerHolder;
        //}
    }

    public static void checkValidCycle(ClientLevel world) {
        if (world.getGameTime() % ChestTracker.CONFIG.databaseOptions.destroyedMemoryCheckInterval == 0) {
            MemoryDatabase database = MemoryDatabase.getCurrent();
            if (database != null) {
                if (!world.dimension().location().equals(currentlyCheckedWorldId)) {
                    currentlyCheckedWorldId = world.dimension().location();
                    currentlyCheckedMemories.clear();
                    currentlyCheckedIndex = 0;
                }
                if (currentlyCheckedMemories.size() == 0) {
//                    currentlyCheckedMemories = new ArrayList<>(database.getAllMemories(world.dimension().location()));
                    currentlyCheckedMemories = new ArrayList<>();
                    for (Memory memory : database.getAllMemories(world.dimension().location())) {
                        // Creating a new ArrayList from a ConcurrentHashMap.ValuesView can apparently cause a
                        // NegativeArraySizeInspection, so in an attempt to fix this the ArrayList is getting populated
                        // manually.
                        //noinspection UseBulkOperation
                        currentlyCheckedMemories.add(memory);
                    }
                    currentlyCheckedIndex = currentlyCheckedMemories.size() - 1;
                }
                if (currentlyCheckedIndex >= 0) {
                    Memory memory = currentlyCheckedMemories.get(currentlyCheckedIndex);
                    if (memory != null) {
                        if (!checkExistsInWorld(memory, world)) {
                            database.removePos(world.dimension().location(), memory.getPosition());
                        }
                        if (ChestTracker.CONFIG.miscOptions.rememberNewChests && memory.getTitle() == null && memory.getItems().size() == 0) {
                            database.removePos(world.dimension().location(), memory.getPosition());
                        }
                    }
                    currentlyCheckedMemories.remove(currentlyCheckedIndex);
                    currentlyCheckedIndex--;
                }
            }
        }
    }

    public static void setWasEnderchest(boolean wasEnderchest) {
        MemoryUtils.wasEnderchest = wasEnderchest;
    }

    public static boolean wasLastEnderchest() {
        return wasEnderchest;
    }
}