package me.aleksilassila.litematica.printer.utils.mods;

import net.fabricmc.loader.api.FabricLoader;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public final class TakeItOutUtils {
    private static final String MOD_ID = "takeitout";
    private static final String CLIENT_CLASS = "net.maxbel.takeitout.client.TakeitoutClient";
    private static final String SOURCES_CLASS = "net.maxbel.takeitout.client.WorldContainerSources";
    private static final String SHULKER_PAYLOAD_CLASS = "net.maxbel.takeitout.Takeitout$GetShulkerStackPayload";
    private static final String CLIENT_PLAY_NETWORKING_CLASS = "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking";
    private static final Minecraft client = Minecraft.getInstance();
    private static boolean apiFailureLogged;

    private TakeItOutUtils() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isAutoTakeoutEnabled() {
        if (!isLoaded()) {
            return false;
        }
        try {
            Field field = Class.forName(CLIENT_CLASS).getField("AUTOTAKEOUT");
            return field.getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("读取自动取货配置", exception);
            return false;
        }
    }

    public static Set<Item> getAvailableItems() {
        if (!isLoaded() || !isAutoTakeoutEnabled()) {
            return Set.of();
        }
        try {
            Object value = Class.forName(CLIENT_CLASS)
                    .getField("WORLD_CONTAINER_ITEMS")
                    .get(null);
            if (!(value instanceof Iterable<?> entries)) {
                return Set.of();
            }
            Set<Item> items = new HashSet<>();
            for (Object entry : entries) {
                if (entry == null) {
                    continue;
                }
                Method stackMethod = entry.getClass().getMethod("stack");
                Method countMethod = entry.getClass().getMethod("count");
                Object stackValue = stackMethod.invoke(entry);
                Object countValue = countMethod.invoke(entry);
                if (stackValue instanceof ItemStack stack
                        && countValue instanceof Integer count
                        && count > 0
                        && !stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            }
            return items.isEmpty() ? Set.of() : Set.copyOf(items);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("读取远程容器材料", exception);
            return Set.of();
        }
    }

    /** Requests Take It Out to refresh its linked-container material cache. */
    public static void requestAvailableItemsRefresh() {
        if (!isLoaded() || !isAutoTakeoutEnabled()) {
            return;
        }
        try {
            Class.forName("net.maxbel.takeitout.client.WorldContainerMaterialListCache")
                    .getMethod("requestRefresh", Minecraft.class)
                    .invoke(null, client);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Older Take It Out versions do not expose the material-list cache API.
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("刷新远程容器材料", exception);
        }
    }

    public static boolean isAwaitingStack() {
        ItemStack awaitingStack = getAwaitingStack();
        if (awaitingStack != null && !awaitingStack.isEmpty()) {
            return true;
        }
        return false;
    }

    public static boolean isAwaitingItem(Item item) {
        ItemStack awaitingStack = getAwaitingStack();
        return item != null && !awaitingStack.isEmpty() && awaitingStack.is(item);
    }

    public static boolean tryRequestItem(Item item) {
        return tryRequestItem(item, false);
    }

    public static boolean tryRequestItem(Item item, boolean emptyShulkerOnly) {
        if (item == null || !isAutoTakeoutEnabled()) {
            return false;
        }
        if (isAwaitingStack()) {
            return true;
        }

        ItemStack required = new ItemStack(item);
        if (required.isEmpty()) {
            return false;
        }
        return tryRequestFromInventoryShulker(required, emptyShulkerOnly)
                || tryRequestFromWorldContainers(emptyShulkerOnly ? emptyWorldContainerRequest(required) : required);
    }

    private static ItemStack emptyWorldContainerRequest(ItemStack required) {
        // Refresh asynchronously so a missing/stale variant can recover on the next attempt,
        // even when render-only inventory tracking and the material list UI are disabled.
        requestAvailableItemsRefresh();
        try {
            Object entries = Class.forName(CLIENT_CLASS).getField("WORLD_CONTAINER_ITEMS").get(null);
            if (entries instanceof Iterable<?> iterable) return selectEmptyShulkerRequest(required, iterable);
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            // Older versions without the material cache can still request an ordinary empty box.
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("选择远程空潜影盒", exception);
        }
        return singleStack(required);
    }

    static ItemStack selectEmptyShulkerRequest(ItemStack required, Iterable<?> entries) throws ReflectiveOperationException {
        for (Object entry : entries) {
            if (entry == null) continue;
            Object stackValue = entry.getClass().getMethod("stack").invoke(entry);
            Object countValue = entry.getClass().getMethod("count").invoke(entry);
            if (stackValue instanceof ItemStack stack && countValue instanceof Integer count
                    && count > 0 && stack.is(required.getItem()) && MaterialRequest.isEmptyShulker(stack)) {
                // Take It Out matches shulker components exactly on the server, including names.
                return singleStack(stack);
            }
        }
        return singleStack(required);
    }

    public static void resetPending() {
        if (!isLoaded()) {
            return;
        }
        try {
            setAwaitingStack(ItemStack.EMPTY);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean tryRequestFromWorldContainers(ItemStack required) {
        try {
            Class<?> sourcesClass = Class.forName(SOURCES_CLASS);
            Method requestStack;
            Object result;
            try {
                requestStack = sourcesClass.getMethod(
                        "requestStack",
                        Minecraft.class,
                        ItemStack.class,
                        boolean.class,
                        boolean.class
                );
                result = requestStack.invoke(null, client, singleStack(required), isSingleItemMode(), false);
            } catch (NoSuchMethodException ignored) {
                requestStack = sourcesClass.getMethod(
                        "requestStack",
                        Minecraft.class,
                        ItemStack.class,
                        boolean.class
                );
                result = requestStack.invoke(null, client, singleStack(required), isSingleItemMode());
            }
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
            return false;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // 旧版 Take It Out 只有背包潜影盒取货，没有世界容器 API。
            return false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("请求世界容器", exception);
            return false;
        }
    }

    private static boolean tryRequestFromInventoryShulker(ItemStack required, boolean emptyShulkerOnly) {
        if (client.player == null) {
            return false;
        }
        Inventory inventory = client.player.getInventory();
        int size = Math.min(36, inventory.getContainerSize());
        for (int shulkerSlot = 0; shulkerSlot < size; shulkerSlot++) {
            ItemStack shulker = inventory.getItem(shulkerSlot);
            if (!isSingleShulker(shulker)) {
                continue;
            }
            int innerSlot = findStoredItemSlot(shulker, required.getItem(), emptyShulkerOnly);
            if (innerSlot == -1) {
                continue;
            }
            return sendShulkerRequest(innerSlot, shulkerSlot, required);
        }
        return false;
    }

    private static boolean sendShulkerRequest(int innerSlot, int shulkerSlot, ItemStack required) {
        try {
            Class<?> payloadClass = Class.forName(SHULKER_PAYLOAD_CLASS);
            Object payload = createShulkerPayload(payloadClass, innerSlot, shulkerSlot);
            if (!canSend(payloadClass)) {
                return false;
            }
            setAwaitingStack(singleStack(required));
            sendPayload(payload);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("请求背包潜影盒", exception);
            clearAwaitingStackIfSameItem(required);
            return false;
        }
    }

    private static Object createShulkerPayload(
            Class<?> payloadClass,
            int innerSlot,
            int shulkerSlot
    ) throws ReflectiveOperationException {
        try {
            Constructor<?> constructor = payloadClass.getConstructor(
                    int.class,
                    int.class,
                    boolean.class
            );
            return constructor.newInstance(innerSlot, shulkerSlot, isSingleItemMode());
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = payloadClass.getConstructor(int.class, int.class);
            return constructor.newInstance(innerSlot, shulkerSlot);
        }
    }

    private static boolean canSend(Class<?> payloadClass) {
        try {
            Field idField = payloadClass.getField("ID");
            Object id = idField.get(null);
            Class<?> networkingClass = Class.forName(CLIENT_PLAY_NETWORKING_CLASS);
            for (Method method : networkingClass.getMethods()) {
                if (!method.getName().equals("canSend") || method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isInstance(id)) {
                    continue;
                }
                Object result = method.invoke(null, id);
                return Boolean.TRUE.equals(result);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("检查网络通道", exception);
            return false;
        }
        return false;
    }

    private static void sendPayload(Object payload) throws ReflectiveOperationException {
        Class<?> networkingClass = Class.forName(CLIENT_PLAY_NETWORKING_CLASS);
        for (Method method : networkingClass.getMethods()) {
            if (!method.getName().equals("send")
                    || method.getParameterCount() != 1
                    || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(payload)) {
                method.invoke(null, payload);
                return;
            }
        }
        throw new NoSuchMethodException("ClientPlayNetworking.send(payload)");
    }

    private static boolean isSingleItemMode() {
        try {
            Field field = Class.forName(CLIENT_CLASS).getField("TAKE_SINGLE_ITEM_MODE");
            return field.getBoolean(null);
        } catch (NoSuchFieldException ignored) {
            // 旧版 payload 不支持单物品模式。
            return false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("读取单物品模式", exception);
            return false;
        }
    }

    /** Returns the mod-owned object so the adapter can identify the request it actually sent. */
    public static ItemStack getAwaitingStack() {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }
        try {
            Object value = Class.forName(CLIENT_CLASS).getField("awaitingStack").get(null);
            return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logApiFailure("读取等待物品", exception);
            return ItemStack.EMPTY;
        }
    }

    private static void setAwaitingStack(ItemStack stack) throws ReflectiveOperationException {
        Class.forName(CLIENT_CLASS).getField("awaitingStack").set(null, stack.copy());
    }

    private static void clearAwaitingStackIfSameItem(ItemStack stack) {
        ItemStack awaitingStack = getAwaitingStack();
        if (awaitingStack.isEmpty() || !awaitingStack.is(stack.getItem())) {
            return;
        }
        try {
            setAwaitingStack(ItemStack.EMPTY);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isSingleShulker(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getCount() == 1
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static int findStoredItemSlot(ItemStack shulker, Item item, boolean emptyShulkerOnly) {
        NonNullList<ItemStack> storedItems = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(shulker, -1);
        for (int slot = 0; slot < storedItems.size(); slot++) {
            ItemStack stack = storedItems.get(slot);
            if (!stack.isEmpty() && stack.is(item) && (!emptyShulkerOnly || MaterialRequest.isEmptyShulker(stack))) {
                return slot;
            }
        }
        return -1;
    }

    private static ItemStack singleStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static void logApiFailure(String operation, Throwable exception) {
        if (apiFailureLogged) {
            return;
        }
        apiFailureLogged = true;
        Reference.LOGGER.warn("Take It Out API 调用异常，{}失败；已跳过该取货路径", operation, exception);
    }
}
