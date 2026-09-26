package me.aleksilassila.litematica.printer.compat;

import me.aleksilassila.litematica.printer.utils.ModUtils;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Compatibility layer for QuickShulker (mod ID: quickshulker).
 * <p>
 * QuickShulker allows opening shulker boxes directly from the inventory
 * by right-clicking or pressing a key. This class provides the reflection
 * bridge so the printer can request QuickShulker to open a shulker on
 * behalf of the player.
 * <p>
 * All interactions use reflection. This class compiles and runs safely
 * when QuickShulker is not installed.
 */
public class QuickShulkerCompat {
    private static boolean resolved = false;

    @Nullable
    private static Method checkAndSendMethod;

    private static final String CLIENT_UTIL = "net.kyrptonaught.quickshulker.client.ClientUtil";

    /**
     * Resolve reflection handles (idempotent).
     */
    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!ModUtils.isQuickShulkerLoaded()) return;

        try {
            Class<?> clientUtil = Class.forName(CLIENT_UTIL);
            checkAndSendMethod = clientUtil.getMethod("CheckAndSend", ItemStack.class, int.class);
        } catch (Exception ignored) {
            checkAndSendMethod = null;
        }
    }

    /**
     * Open a shulker box via QuickShulker's {@code CheckAndSend} API.
     *
     * @param stack         the shulker box ItemStack
     * @param inventorySlot the inventory slot the shulker resides in
     */
    public static void openShulker(ItemStack stack, int inventorySlot) {
        if (!ModUtils.isQuickShulkerLoaded()) return;
        resolve();
        if (checkAndSendMethod == null) return;

        try {
            checkAndSendMethod.invoke(null, stack, inventorySlot);
        } catch (Exception ignored) {}
    }
}
