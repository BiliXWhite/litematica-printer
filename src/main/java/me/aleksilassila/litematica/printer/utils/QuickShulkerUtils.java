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
import me.aleksilassila.litematica.printer.interfaces.compat.QuickShulkerCompat;
import me.aleksilassila.litematica.printer.interfaces.compat.TakeItOutCompat;
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
    @Setter
    @Getter
    private static int shulkerCooldown;
    @Getter @Setter
    private static int shulkerBoxSlot = -1;
    @Getter
    private static final Set<Item> lastNeedItemList = new HashSet<>();
    private static final LinkedList<ReturnRequest> itemsToReturn = new LinkedList<>();
    private static final List<TrackedShulker> trackedShulkers = new ArrayList<>();
    private static ReturnRequest activeReturnRequest;
    private static TrackedShulker activeShulker;
    private static AbstractContainerMenu deferredCloseContainer;
    private static int deferredCloseTicks;

    private QuickShulkerUtils() {}

    public static void tick() {
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }
        if (deferredCloseContainer != null && deferredCloseTicks > 0 && --deferredCloseTicks == 0) {
            LocalPlayer player = mc.player;
            if (player == null || player.containerMenu != deferredCloseContainer) {
                clearDeferredClose();
            } else if (deferredCloseContainer.getCarried().isEmpty()) {
                player.closeContainer();
                clearDeferredClose();
            } else {
                deferredCloseTicks = 1;
            }
        }
    }

    public static void addLastNeedItem(Item item) {
        lastNeedItemList.add(item);
    }

    public static void clearLastNeedItems() {
        lastNeedItemList.clear();
    }

    // ========== 统一取物入口 ==========

    /**
     * 根据 ShulkerSource 配置分发潜影盒取物请求。
     * @return true 已发起请求（调用方应等待），false 无法处理
     */
    public static boolean requestShulkerItem(LocalPlayer player, Item[] items) {
        if (!Configs.Print.USE_QUICK_SHULKER.getBooleanValue()) return false;

        ShulkerSource source = (ShulkerSource) Configs.Print.SHULKER_SOURCE.getOptionListValue();

        //#if MC >= 260102
        if (source == ShulkerSource.TAKE_IT_OUT) {
            return TakeItOutCompat.tryExtract(player, items);
        }
        //#endif

        if (source == ShulkerSource.MOD && !QUICK_SHULKER_LOADED) {
            return false;
        }

        if (isOpenHandler || shulkerCooldown > 0) return false;

        Inventory inventory = player.getInventory();

        if (Configs.Print.RETURN_TO_SHULKER_WHEN_FULL.getBooleanValue()
                && isInventoryFull(inventory)) {
            ReturnRequest returnRequest = itemsToReturn.peekFirst();
            if (returnRequest == null) return false;

            int shulkerSlot = findReturnShulker(inventory, returnRequest);
            if (shulkerSlot == -1) return false;

            activeReturnRequest = returnRequest;
            activeShulker = returnRequest.shulker();
            return openSelectedShulker(inventory, shulkerSlot, source);
        }

        // 不开启精确回塞时，只要有 itemsToReturn 就尝试回塞到任意有空位的潜影盒
        if (!Configs.Print.RETURN_TO_SHULKER_WHEN_FULL.getBooleanValue()
                && isInventoryFull(inventory)) {
            ReturnRequest returnRequest = itemsToReturn.peekFirst();
            if (returnRequest == null) return false;

            int shulkerSlot = findAnyShulker(player);
            if (shulkerSlot == -1) return false;

            activeReturnRequest = returnRequest;
            activeShulker = null;
            return openSelectedShulker(inventory, shulkerSlot, source);
        }

        for (Item item : items) {
            int shulkerSlot = findShulkerWithItem(player, item);
            if (shulkerSlot != -1) {
                ItemStack shulkerStack = inventory.getItem(shulkerSlot);
                activeReturnRequest = null;
                activeShulker = findTrackedShulker(shulkerStack);
                if (activeShulker == null) {
                    activeShulker = new TrackedShulker(shulkerStack.getItem(), getShulkerContents(shulkerStack));
                    trackedShulkers.add(activeShulker);
                }
                activeShulker.setLastKnownSlot(shulkerSlot);
                clearLastNeedItems();
                addLastNeedItem(item);
                return openSelectedShulker(inventory, shulkerSlot, source);
            }
        }
        return false;
    }

    private static boolean openSelectedShulker(Inventory inventory, int shulkerSlot, ShulkerSource source) {
        ItemStack shulkerStack = inventory.getItem(shulkerSlot);
        setShulkerBoxSlot(shulkerSlot);
        ModUtils.closeScreen++;
        setOpenHandler(true);
        setShulkerCooldown(Configs.Print.SHULKER_COOLDOWN.getIntegerValue());

        // 按来源打开潜影盒：PLUGIN 走右键模拟，MOD 走 QuickShulker API
        if (source == ShulkerSource.PLUGIN) {
            openShulkerByRightClick(shulkerSlot);
        } else {
            QuickShulkerCompat.openShulker(shulkerStack, shulkerSlot);
        }
        return true;
    }

    // ========== 容器槽位点击 ==========

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

    public static void pickupSlot(AbstractContainerMenu container, int slotIndex) {
        clickSlot(container, slotIndex, 0, ClickType.PICKUP);
    }

    /** button 即目标快捷栏槽位 0-8 */
    public static void swapWithHotbar(AbstractContainerMenu container, int slotIndex, int hotbarSlot) {
        clickSlot(container, slotIndex, hotbarSlot, ClickType.SWAP);
    }

    // ========== 插件服右键开箱 ==========

    public static void openShulkerByRightClick(int inventorySlot) {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                inventorySlot,
                1, // 右键
                ClickType.PICKUP,
                mc.player);
    }

    // ========== 潜影盒取物逻辑 ==========
    /**
     * 收到 ContainerSetContent 包时调用。
     * 背包满时只执行归还；背包未满时执行正常取物。
     */
    public static void switchFromShulker() {
        if (deferredCloseContainer != null) return;

        LocalPlayer player = mc.player;
        if (player == null || player.containerMenu.equals(player.inventoryMenu)) {
            isOpenHandler = false;
            return;
        }

        AbstractContainerMenu container = player.containerMenu;
        Inventory inventory = player.getInventory();

        if (activeReturnRequest != null) {
            boolean inventoryTransferPerformed =
                    returnItemToShulker(player, container, inventory, activeReturnRequest);
            finishShulkerOperation(player, container, inventoryTransferPerformed);
            return;
        }

        int ownSlots = container.slots.size() - 36;
        for (int slotIndex = 0; slotIndex < ownSlots; slotIndex++) {
            Slot slot = container.slots.get(slotIndex);
            if (!slot.hasItem()) continue;
            for (Item item : lastNeedItemList) {
                if (slot.getItem().getItem().equals(item)) {
                    Item returnItem = slot.getItem().getItem();
                    // 找背包空位（优先快捷栏，再主背包）
                    int emptyInvSlot = -1;
                    for (int i = 0; i < Math.min(inventory.getContainerSize(), 36); i++) {
                        if (inventory.getItem(i).isEmpty()) {
                            emptyInvSlot = i;
                            break;
                        }
                    }
                    if (emptyInvSlot != -1 && mc.gameMode != null) {
                        // 背包索引 → 容器槽位索引的转换
                        int containerTarget;
                        if (emptyInvSlot < 9) {
                            // 快捷栏在容器末尾
                            containerTarget = ownSlots + 27 + emptyInvSlot;
                        } else {
                            // 主背包紧接容器自身槽位
                            containerTarget = ownSlots + (emptyInvSlot - 9);
                        }
                        // 先拾取潜影盒槽位，再放到目标槽位
                        mc.gameMode.handleInventoryMouseClick(container.containerId, slot.index, 0, ClickType.PICKUP, player);
                        mc.gameMode.handleInventoryMouseClick(container.containerId, containerTarget, 0, ClickType.PICKUP, player);
                        if (activeShulker != null) {
                            activeShulker.updateContents(getContainerContents(container, ownSlots));
                            itemsToReturn.addLast(new ReturnRequest(returnItem, activeShulker));
                        }
                        finishShulkerOperation(player, container, true);
                    } else {
                        finishShulkerOperation(player, container, false);
                    }
                    return;
                }
            }
        }

        finishShulkerOperation(player, container, false);
    }

    private static boolean returnItemToShulker(LocalPlayer player, AbstractContainerMenu container,
                                               Inventory inventory, ReturnRequest returnRequest) {
        if (mc.gameMode == null) {
            itemsToReturn.removeFirstOccurrence(returnRequest);
            return false;
        }

        int ownSlots = container.slots.size() - 36;
        for (int i = 0; i < Math.min(inventory.getContainerSize(), 36); i++) {
            if (!inventory.getItem(i).is(returnRequest.item())) continue;

            for (int shulkerSlot = 0; shulkerSlot < ownSlots; shulkerSlot++) {
                if (container.slots.get(shulkerSlot).hasItem()) continue;

                int containerSource = i < 9 ? ownSlots + 27 + i : ownSlots + i - 9;
                mc.gameMode.handleInventoryMouseClick(container.containerId, containerSource, 0, ClickType.PICKUP, player);
                mc.gameMode.handleInventoryMouseClick(container.containerId, shulkerSlot, 0, ClickType.PICKUP, player);
                itemsToReturn.removeFirstOccurrence(returnRequest);
                if (returnRequest.shulker() != null) {
                    returnRequest.shulker().updateContents(getContainerContents(container, ownSlots));
                }
                return true;
            }
            // 潜影盒无空位 → 移除失效请求，避免死循环
            itemsToReturn.removeFirstOccurrence(returnRequest);
            return false;
        }
        // 物品已不在背包（已被使用）→ 移除失效请求，避免死循环
        itemsToReturn.removeFirstOccurrence(returnRequest);
        return false;
    }

    private static void finishShulkerOperation(LocalPlayer player, AbstractContainerMenu container,
                                               boolean inventoryTransferPerformed) {
        boolean wasReturn = activeReturnRequest != null;
        if (!inventoryTransferPerformed && container.getCarried().isEmpty()) {
            player.closeContainer();
            clearDeferredClose();
        } else {
            deferredCloseContainer = container;
            deferredCloseTicks = inventoryTransferPerformed ? 2 : 1;
        }
        shulkerBoxSlot = -1;
        activeReturnRequest = null;
        activeShulker = null;
        lastNeedItemList.clear();
        if (itemsToReturn.isEmpty()) trackedShulkers.clear();
        // 回塞完成后立即允许下一次潜影盒操作，避免当前打印位置因冷却被跳过
        if (wasReturn) shulkerCooldown = 0;
    }

    private static void clearDeferredClose() {
        deferredCloseContainer = null;
        deferredCloseTicks = 0;
        isOpenHandler = false;
    }

    /** 在玩家背包（跳过快捷栏）中找到包含目标物品的潜影盒，返回背包槽位索引，未找到返回 -1 */
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

    private static int findTrackedShulker(Inventory inventory, TrackedShulker trackedShulker) {
        for (int i = 9; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem().equals(trackedShulker.boxItem())
                    && sameContents(getShulkerContents(stack), trackedShulker.contents())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 三级查找回塞目标潜影盒：
     * 1. 精确内容匹配（最准）
     * 2. 检查记录的 lastKnownSlot 是否还是同类型潜影盒（防NBT漂移）
     * 3. 查找背包里装有同种物品的其他潜影盒（兜底）
     * 关闭精确回塞时退化为"只要有空位就塞"
     */
    private static int findReturnShulker(Inventory inventory, ReturnRequest request) {
        TrackedShulker tracked = request.shulker();

        // L1: 精确匹配
        int slot = findTrackedShulker(inventory, tracked);
        if (slot != -1) return slot;

        // L2: 检查记录的槽位
        int lastSlot = tracked.lastKnownSlot();
        if (lastSlot >= 9 && lastSlot < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(lastSlot);
            if (stack.getItem().equals(tracked.boxItem())) {
                return lastSlot;
            }
        }

        // L3: 查找装有同种物品且有空位的潜影盒
        for (int i = 9; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (!id.contains("shulker_box") || stack.getCount() != 1) continue;
            List<ItemStack> contents = getShulkerContents(stack);
            if (contents.size() >= 27) continue;
            if (contents.stream().anyMatch(s -> s.getItem().equals(request.item()))) {
                return i;
            }
        }

        return -1;
    }

    private static int findAnyShulker(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 9; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (id.contains("shulker_box") && stack.getCount() == 1) {
                return i;
            }
        }
        return -1;
    }

    private static TrackedShulker findTrackedShulker(ItemStack stack) {
        List<ItemStack> contents = getShulkerContents(stack);
        for (TrackedShulker trackedShulker : trackedShulkers) {
            if (stack.getItem().equals(trackedShulker.boxItem())
                    && sameContents(contents, trackedShulker.contents())) {
                return trackedShulker;
            }
        }
        return null;
    }

    private static List<ItemStack> getShulkerContents(ItemStack stack) {
        return copyNonEmptyStacks(fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1));
    }

    private static List<ItemStack> getContainerContents(AbstractContainerMenu container, int ownSlots) {
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < ownSlots; i++) {
            ItemStack stack = container.slots.get(i).getItem();
            if (!stack.isEmpty()) contents.add(stack.copy());
        }
        return contents;
    }

    private static List<ItemStack> copyNonEmptyStacks(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) copies.add(stack.copy());
        }
        return copies;
    }

    private static boolean sameContents(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;

        boolean[] matched = new boolean[second.size()];
        for (ItemStack firstStack : first) {
            boolean found = false;
            for (int i = 0; i < second.size(); i++) {
                if (!matched[i] && fi.dy.masa.malilib.util.InventoryUtils
                        .areStacksEqual(firstStack, second.get(i))) {
                    matched[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean isInventoryFull(Inventory inventory) {
        for (int i = 0; i < Math.min(inventory.getContainerSize(), 36); i++) {
            if (inventory.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private record ReturnRequest(Item item, TrackedShulker shulker) {}

    private static final class TrackedShulker {
        private final Item boxItem;
        private List<ItemStack> contents;
        private int lastKnownSlot = -1;

        private TrackedShulker(Item boxItem, List<ItemStack> contents) {
            this.boxItem = boxItem;
            this.contents = contents;
        }

        private Item boxItem() {
            return boxItem;
        }

        private List<ItemStack> contents() {
            return contents;
        }

        private int lastKnownSlot() {
            return lastKnownSlot;
        }

        private void setLastKnownSlot(int slot) {
            this.lastKnownSlot = slot;
        }

        private void updateContents(List<ItemStack> contents) {
            this.contents = contents;
        }
    }
}
