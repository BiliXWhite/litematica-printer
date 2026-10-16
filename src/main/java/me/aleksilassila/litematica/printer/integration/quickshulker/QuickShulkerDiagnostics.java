package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.Reference;
import net.minecraft.world.item.ItemStack;

/** Opt in with -Dlitematica.printer.inventoryDebug=true; never logs container contents. */
public final class QuickShulkerDiagnostics {
    private static final java.util.Map<net.minecraft.core.BlockPos, Long> recentPlacements = new java.util.LinkedHashMap<>();
    private static final boolean ENABLED = Boolean.getBoolean("litematica.printer.inventoryDebug");
    private QuickShulkerDiagnostics() { }
    public static boolean enabled() { return ENABLED; }
    public static void log(String event) {
        if (ENABLED) Reference.LOGGER.info("[PrinterInventory] {}", event);
    }
    public static void placement(net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos target,
                                 net.minecraft.core.Direction side, net.minecraft.world.item.Item[] expected) {
        if (!ENABLED) return;
        long now = System.nanoTime();
        recentPlacements.entrySet().removeIf(entry -> now - entry.getValue() > 10_000_000_000L);
        while (recentPlacements.size() > 254) recentPlacements.remove(recentPlacements.keySet().iterator().next());
        recentPlacements.put(target.immutable(), now);
        recentPlacements.put(target.relative(side), now);
        log("place-send target=" + target + " side=" + side + " selected="
                + me.aleksilassila.litematica.printer.utils.InventoryUtils.getSelectedSlot(player.getInventory())
                + " held=" + stack(player.getMainHandItem()) + " expected=" + java.util.Arrays.toString(expected)
                + " menu=" + player.containerMenu.containerId);
    }

    public static void blockReply(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!ENABLED) return;
        Long sentAt = recentPlacements.get(pos);
        if (sentAt != null && System.nanoTime() - sentAt <= 10_000_000_000L) {
            log("server-block pos=" + pos + " state=" + state);
        }
    }

    public static String stack(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getItem() + "x" + stack.getCount();
    }
}
