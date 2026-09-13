package me.aleksilassila.litematica.printer.utils;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.I18n;
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
    private static final int OPERATION_TIMEOUT_TICKS = 100;
    private static int operationTicks;
    private static LocalPlayer operationPlayer;
    private static PendingTransfer pendingTransfer;

    private QuickShulkerUtils() {}

    public static void tick() {
        if (operationPlayer != null && operationPlayer != mc.player) {
            resetOperation();
            itemsToReturn.clear();
            trackedShulkers.clear();
        }
        if (shulkerCooldown > 0) shulkerCooldown--;
        if (!isOpenHandler) return;

        LocalPlayer player = mc.player;
        if (pendingTransfer != null && player != null) {
            if (player.containerMenu != pendingTransfer.container()) {
                failOperation(player);
                return;
            }
            if (pendingTransfer.isConfirmed(player.getInventory())) {
                if (activeShulker != null) {
                    activeShulker.updateContents(getContainerContents(player.containerMenu,
                            player.containerMenu.slots.size() - 36));
                }
                if (activeReturnRequest != null) {
                    itemsToReturn.removeFirstOccurrence(activeReturnRequest);
                } else if (activeShulker != null) {
                    itemsToReturn.addLast(new ReturnRequest(pendingTransfer.item(), activeShulker));
                }
                finishShulkerOperation(player);
                return;
            }
        }
        if (++operationTicks >= OPERATION_TIMEOUT_TICKS) failOperation(player);
    }

    private static void failOperation(LocalPlayer player) {
        // 只有已发出的转移结果未确认时才停机。
        if (pendingTransfer != null) Configs.Core.WORK_SWITCH.setBooleanValue(false);
        MessageUtils.setOverlayMessage((pendingTransfer != null
                ? I18n.SHULKER_SYNC_TIMEOUT : I18n.SHULKER_TRANSFER_NOT_STARTED).getName());
        finishShulkerOperation(player);
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

        if (isOpenHandler || shulkerCooldown > 0 || player.containerMenu != player.inventoryMenu) return false;

        Inventory inventory = player.getInventory();

        if (isInventoryFull(inventory)) {
            ReturnRequest returnRequest = itemsToReturn.peekFirst();
            if (returnRequest == null) return false;

            boolean exactReturn = Configs.Print.RETURN_TO_SHULKER_WHEN_FULL.getBooleanValue();
            int shulkerSlot = exactReturn ? findReturnShulker(inventory, returnRequest) : findAnyShulker(player);
            if (shulkerSlot == -1) return false;

            activeReturnRequest = returnRequest;
            activeShulker = exactReturn ? returnRequest.shulker() : null;
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
        operationPlayer = mc.player;
        operationTicks = 0;
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
        LocalPlayer player = mc.player;
        if (!isOpenHandler || pendingTransfer != null || player == null
                || player.containerMenu == player.inventoryMenu) return;

        AbstractContainerMenu container = player.containerMenu;
        Inventory inventory = player.getInventory();
        int ownSlots = container.slots.size() - 36;
        if (!container.getCarried().isEmpty()) {
            failOperation(player);
            return;
        }

        if (activeReturnRequest != null) {
            for (int i = 0; i < Math.min(inventory.getContainerSize(), 36); i++) {
                if (inventory.getItem(i).is(activeReturnRequest.item())) {
                    beginTransfer(container, i < 9 ? ownSlots + 27 + i : ownSlots + i - 9, inventory);
                    return;
                }
            }
            itemsToReturn.removeFirstOccurrence(activeReturnRequest);
        } else {
            for (int slotIndex = 0; slotIndex < ownSlots; slotIndex++) {
                ItemStack stack = container.slots.get(slotIndex).getItem();
                if (!stack.isEmpty() && lastNeedItemList.contains(stack.getItem())) {
                    beginTransfer(container, slotIndex, inventory);
                    return;
                }
            }
        }
        finishShulkerOperation(player);
    }

    private static void beginTransfer(AbstractContainerMenu container, int sourceSlot, Inventory inventory) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            failOperation(mc.player);
            return;
        }
        if (!container.slots.get(sourceSlot).mayPickup(mc.player)) {
            failOperation(mc.player);
            return;
        }
        if (!hasTransferSpace(container, sourceSlot)) {
            // 未发包，无需等待同步；丢弃无法回塞的请求，避免反复打开同一个满盒。
            itemsToReturn.removeFirstOccurrence(activeReturnRequest);
            MessageUtils.setOverlayMessage(I18n.SHULKER_NO_SPACE.getName());
            finishShulkerOperation(mc.player);
            return;
        }
        ItemStack stack = container.slots.get(sourceSlot).getItem();
        PendingTransfer transfer = new PendingTransfer(container, sourceSlot, stack.getItem(), stack.getCount(),
                countItem(inventory, stack.getItem()), countContainerItem(container, stack.getItem()),
                activeReturnRequest != null);

        // 不预测库存变动，让服务端回传变动槽位后再确认取货或回塞。
        //#if MC >= 12105
        connection.send(new ServerboundContainerClickPacket(
                container.containerId, container.getStateId(), Shorts.checkedCast(sourceSlot),
                (byte) 0, ClickType.QUICK_MOVE, new Int2ObjectOpenHashMap<>(),
                HashedStack.create(container.getCarried(), connection.decoratedHashOpsGenenerator())));
        //#else
        //$$ connection.send(new ServerboundContainerClickPacket(
        //$$         container.containerId, container.getStateId(), sourceSlot, 0, ClickType.QUICK_MOVE,
        //$$         container.getCarried().copy(), new Int2ObjectOpenHashMap<>()));
        //#endif
        pendingTransfer = transfer;
        operationTicks = 0;
    }

    private static boolean hasTransferSpace(AbstractContainerMenu container, int sourceSlot) {
        ItemStack stack = container.slots.get(sourceSlot).getItem();
        if (stack.isEmpty()) return false;
        int ownSlots = container.slots.size() - 36;
        boolean returning = sourceSlot >= ownSlots;
        for (int i = returning ? 0 : ownSlots; i < (returning ? ownSlots : container.slots.size()); i++) {
            Slot slot = container.slots.get(i);
            ItemStack target = slot.getItem();
            if (!slot.mayPlace(stack) || target.getCount() >= slot.getMaxStackSize(stack)) continue;
            if (target.isEmpty() || stack.isStackable() &&
                    //#if MC >= 12005
                    ItemStack.isSameItemSameComponents(stack, target)
                    //#else
                    //$$ ItemStack.isSameItemSameTags(stack, target)
                    //#endif
            ) return true;
        }
        return false;
    }

    private static int countItem(Inventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < Math.min(inventory.getContainerSize(), 36); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int countContainerItem(AbstractContainerMenu container, Item item) {
        int count = 0;
        for (int i = 0; i < container.slots.size() - 36; i++) {
            ItemStack stack = container.slots.get(i).getItem();
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private record PendingTransfer(AbstractContainerMenu container, int sourceSlot, Item item,
                                   int sourceCount, int inventoryCount, int containerCount, boolean returning) {
        private boolean isConfirmed(Inventory inventory) {
            ItemStack source = container.slots.get(sourceSlot).getItem();
            if (!source.isEmpty() && !source.is(item)) return false;
            int moved = sourceCount - (source.is(item) ? source.getCount() : 0);
            int inventoryDelta = countItem(inventory, item) - inventoryCount;
            int containerDelta = countContainerItem(container, item) - containerCount;
            return moved > 0 && inventoryDelta == (returning ? -moved : moved)
                    && containerDelta == -inventoryDelta && container.getCarried().isEmpty();
        }
    }

    private static void finishShulkerOperation(LocalPlayer player) {
        boolean wasReturn = activeReturnRequest != null;
        if (player != null && player.containerMenu != player.inventoryMenu
                && (pendingTransfer == null || player.containerMenu == pendingTransfer.container())) {
            player.closeContainer();
        }
        resetOperation();
        if (itemsToReturn.isEmpty()) trackedShulkers.clear();
        if (wasReturn) shulkerCooldown = 0;
    }

    private static void resetOperation() {
        shulkerBoxSlot = -1;
        isOpenHandler = false;
        activeReturnRequest = null;
        activeShulker = null;
        pendingTransfer = null;
        operationPlayer = null;
        operationTicks = 0;
        lastNeedItemList.clear();
        ModUtils.closeScreen = 0;
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