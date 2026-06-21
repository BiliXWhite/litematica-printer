package me.aleksilassila.litematica.printer.utils;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ShulkerSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

//#if MC >= 12105
import net.minecraft.network.HashedStack;
//#endif

import java.util.*;

public class QuickShulkerUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final boolean QUICK_SHULKER_LOADED = FabricLoader.getInstance().isModLoaded("quickshulker");

    @Getter @Setter
    private static boolean isOpenHandler;
    private static int shulkerCooldown;
    @Getter @Setter
    private static int shulkerBoxSlot = -1;
    private static final Set<Item> lastNeedItemList = new HashSet<>();
    /** Items previously extracted from the shulker that should be returned when inventory is full. */
    private static final LinkedList<Item> itemsToReturn = new LinkedList<>();

    private QuickShulkerUtils() {}

    // ========== Mod Detection ==========

    public static boolean isQuickShulkerLoaded() {
        return QUICK_SHULKER_LOADED;
    }

    // ========== Tick / Cooldown ==========

    public static void tick() {
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }
    }

    // ========== State Accessors ==========

    public static int getShulkerCooldown() {
        return shulkerCooldown;
    }

    public static void setShulkerCooldown(int cooldown) {
        shulkerCooldown = cooldown;
    }

    public static Set<Item> getLastNeedItemList() {
        return lastNeedItemList;
    }

    public static void addLastNeedItem(Item item) {
        lastNeedItemList.add(item);
    }

    public static void clearLastNeedItems() {
        lastNeedItemList.clear();
    }

    // ========== Open Shulker (dispatch by ShulkerSource config) ==========

    /**
     * Open a shulker box at the given inventory slot, using the method
     * configured by {@link Configs.Print#SHULKER_SOURCE}.
     * <ul>
     *   <li>{@link ShulkerSource#MOD} — uses QuickShulker mod API (CheckAndSend)</li>
     *   <li>{@link ShulkerSource#PLUGIN} — right-clicks on the slot for plugin servers</li>
     * </ul>
     */
    public static void openShulker(ItemStack stack, int inventorySlot) {
        ShulkerSource source = (ShulkerSource) Configs.Print.SHULKER_SOURCE.getOptionListValue();
        switch (source) {
            case PLUGIN:
                openShulkerByRightClick(inventorySlot);
                break;
            case MOD:
            default:
                if (!QUICK_SHULKER_LOADED) {
                    isOpenHandler = false;
                    shulkerBoxSlot = -1;
                    return;
                }
                openShulkerViaMod(stack, inventorySlot);
                break;
        }
    }

    /** Open via QuickShulker mod's ClientUtil.CheckAndSend API. */
    private static void openShulkerViaMod(ItemStack stack, int inventorySlot) {
        if (!QUICK_SHULKER_LOADED) return;
        try {
            Class<?> clientUtil = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
            clientUtil.getMethod("CheckAndSend", ItemStack.class, int.class)
                    .invoke(null, stack, inventorySlot);
        } catch (Exception ignored) {}
    }

    // ====================================================================
    //  Click Slot via ServerboundContainerClickPacket
    // ====================================================================

    /**
     * Click a slot in the given container by sending ServerboundContainerClickPacket.
     *
     * @param container the container to interact with
     * @param slotIndex the slot index to click
     * @param button    the button (0 = left, 1 = right; for SWAP: hotbar slot 0-8)
     * @param type      the click type (PICKUP, SWAP, QUICK_MOVE, etc.)
     */
    public static void clickSlot(AbstractContainerMenu container, int slotIndex, int button, ClickType type) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) return;

        NonNullList<Slot> slots = container.slots;
        int totalSlots = slots.size();
        List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
        for (Slot slotItem : slots) {
            copies.add(slotItem.getItem().copy());
        }

        //#if MC >= 12105
        Int2ObjectMap<HashedStack> snapshot = new Int2ObjectOpenHashMap<>();
        //#else
        //$$ Int2ObjectMap<ItemStack> snapshot = new Int2ObjectOpenHashMap<>();
        //#endif

        for (int j = 0; j < totalSlots; j++) {
            ItemStack original = copies.get(j);
            ItemStack current = slots.get(j).getItem();
            if (!ItemStack.isSameItem(original, current)) {
                //#if MC >= 12105
                snapshot.put(j, HashedStack.create(current, connection.decoratedHashOpsGenenerator()));
                //#else
                //$$ snapshot.put(j, current.copy());
                //#endif
            }
        }

        //#if MC >= 12105
        HashedStack carried = HashedStack.create(container.getCarried(), connection.decoratedHashOpsGenenerator());
        connection.send(new ServerboundContainerClickPacket(
                container.containerId,
                container.getStateId(),
                Shorts.checkedCast(slotIndex),
                SignedBytes.checkedCast(button),
                type,
                snapshot,
                carried
        ));
        //#else
        //$$ connection.send(new ServerboundContainerClickPacket(
        //$$         container.containerId,
        //$$         container.getStateId(),
        //$$         slotIndex,
        //$$         button,
        //$$         type,
        //$$         container.getCarried().copy(),
        //$$         snapshot
        //$$ ));
        //#endif

        container.clicked(slotIndex, button, type, mc.player);
    }

    /**
     * Left-click on a slot to pick up items (or place held items).
     */
    public static void pickupSlot(AbstractContainerMenu container, int slotIndex) {
        clickSlot(container, slotIndex, 0, ClickType.PICKUP);
    }

    /**
     * Swap the contents of a slot with a hotbar slot.
     * Uses ClickType.SWAP, where the button is the target hotbar slot (0-8).
     */
    public static void swapWithHotbar(AbstractContainerMenu container, int slotIndex, int hotbarSlot) {
        clickSlot(container, slotIndex, hotbarSlot, ClickType.SWAP);
    }

    // ====================================================================
    //  Plugin-Server Right-Click Opening (via handleInventoryMouseClick)
    // ====================================================================

    /**
     * Right-click on a slot in the player inventory to trigger server plugin
     * shulker box opening. Plugin servers intercept right-clicks on shulker
     * box slots and open them as containers.
     */
    public static void openShulkerByRightClick(int inventorySlot) {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                inventorySlot,
                1, // right click
                ClickType.PICKUP,
                mc.player);
    }

    // ====================================================================
    //  Shulker Box Extraction Logic
    //  Phase 1 (only when full): return previously-extracted items to shulker
    //  Phase 2: one-way extraction of the needed item
    // ====================================================================

    /**
     * Called when a ContainerSetContent packet is received while the shulker
     * handler is active.
     *
     * <p>Phase 1 — if {@link Configs.Print#RETURN_TO_SHULKER_WHEN_FULL} is
     * enabled and the player's main inventory has no empty slots, items in
     * {@link #itemsToReturn} are swapped back into the shulker one by one.
     *
     * <p>Phase 2 — finds the needed item in the shulker and swaps it into any
     * empty inventory slot (one-way). The next tick, {@code switchToItems}
     * picks it up from the inventory and brings it to hand.
     */
    public static void switchFromShulker() {
        LocalPlayer player = mc.player;
        if (player == null || player.containerMenu.equals(player.inventoryMenu)) {
            isOpenHandler = false;
            return;
        }

        AbstractContainerMenu container = player.containerMenu;
        Inventory inventory = player.getInventory();

        // ── Phase 1: return previously-extracted items when full ──
        if (Configs.Print.RETURN_TO_SHULKER_WHEN_FULL.getBooleanValue()
                && !hasEmptySlot(inventory)) {
            Iterator<Item> it = itemsToReturn.iterator();
            while (it.hasNext()) {
                Item returnItem = it.next();
                for (int i = 0; i < Math.min(inventory.getContainerSize(), 36); i++) {
                    if (inventory.getItem(i).is(returnItem)) {
                        // Find an empty shulker slot to return it to
                        for (Slot s : container.slots) {
                            if (!s.hasItem()) {
                                int ownSlots = container.slots.size() - 36;
                                int containerSource;
                                if (i < 9) {
                                    containerSource = ownSlots + 27 + i;
                                } else {
                                    containerSource = ownSlots + (i - 9);
                                }
                                mc.gameMode.handleInventoryMouseClick(container.containerId, containerSource, 0, ClickType.PICKUP, player);
                                mc.gameMode.handleInventoryMouseClick(container.containerId, s.index, 0, ClickType.PICKUP, player);
                                it.remove();
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }

        // ── Phase 2: one-way extraction into any empty slot ──
        for (Slot slot : container.slots) {
            if (!slot.hasItem()) continue;
            for (Item item : lastNeedItemList) {
                if (slot.getItem().getItem().equals(item)) {
                    itemsToReturn.addLast(slot.getItem().getItem());
                    // Find any empty slot in the inventory (hotbar 0-8 first, then main 9-35)
                    int emptyInvSlot = -1;
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        if (inventory.getItem(i).isEmpty()) {
                            emptyInvSlot = i;
                            break;
                        }
                    }
                    if (emptyInvSlot != -1 && mc.gameMode != null) {
                        // Convert Inventory index to container slot index
                        int ownSlots = container.slots.size() - 36; // container slots before player inventory
                        int containerTarget;
                        if (emptyInvSlot < 9) {
                            // Hotbar: at the end of the container
                            containerTarget = ownSlots + 27 + emptyInvSlot;
                        } else {
                            // Main inventory: right after container's own slots
                            containerTarget = ownSlots + (emptyInvSlot - 9);
                        }
                        // Pick up from shulker slot, then place into target slot
                        mc.gameMode.handleInventoryMouseClick(container.containerId, slot.index, 0, ClickType.PICKUP, player);
                        mc.gameMode.handleInventoryMouseClick(container.containerId, containerTarget, 0, ClickType.PICKUP, player);
                    }
                    player.closeContainer();
                    shulkerBoxSlot = -1;
                    isOpenHandler = false;
                    lastNeedItemList.clear();
                    return;
                }
            }
        }

        player.closeContainer();
        isOpenHandler = false;
        lastNeedItemList.clear();
    }

    /**
     * Find a shulker box in the player's inventory that contains the target item.
     * Searches from slot 9 onwards (skipping the hotbar slots).
     */
    public static int findShulkerWithItem(LocalPlayer player, Item target) {
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (id.contains("shulker_box") && stack.getCount() == 1) {
                NonNullList<ItemStack> contents = fi.dy.masa.malilib.util.InventoryUtils
                        .getStoredItems(stack, -1);
                if (contents.stream().anyMatch(s -> s.getItem().equals(target))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean hasEmptySlot(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!Inventory.isHotbarSlot(i) && inventory.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
