package me.aleksilassila.litematica.printer.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class QuickShulkerUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("quickshulker");

    private QuickShulkerUtils() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean isShulkerScreenOpen() {
        if (!LOADED || mc.player == null) return false;
        try {
            Class<?> invClass = Class.forName("net.kyrptonaught.quickshulker.api.ItemStackInventory");
            return invClass.isInstance(mc.player.containerMenu);
        } catch (Exception e) {
            return false;
        }
    }

    public static void openShulker(ItemStack stack, int inventorySlot) {
        if (!LOADED) return;
        try {
            Class<?> clientUtil = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
            clientUtil.getMethod("CheckAndSend", ItemStack.class, int.class)
                    .invoke(null, stack, inventorySlot);
        } catch (Exception ignored) {}
    }
}

