package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** An open-screen packet is only a candidate; initial contents must match the source box. */
final class QuickShulkerContainerSession {
    private List<ItemStack> expected;
    private boolean clickMode;
    private int candidateId = -1;
    private int ownedId = -1;

    void begin(ItemStack box, boolean clickMode) {
        reset();
        this.clickMode = clickMode;
        this.expected = new ArrayList<>();
        var stored = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(box, 27);
        for (int i = 0; i < 27; i++) {
            expected.add(i < stored.size() ? stored.get(i).copy() : ItemStack.EMPTY);
        }
    }

    void opened(int containerId, MenuType<?> type) {
        if (expected != null && ownedId < 0 && QuickShulkerSupport.acceptsMenu(type, clickMode)) {
            candidateId = containerId;
        }
    }

    boolean confirm(int containerId, List<ItemStack> items, ItemStack carried) {
        if (owns(containerId)) return true;
        if (expected == null || candidateId != containerId || items.size() < 63 || !carried.isEmpty()) return false;
        for (int slot = 0; slot < 27; slot++) {
            if (!ItemStack.matches(expected.get(slot), items.get(slot))) return false;
        }
        ownedId = containerId;
        return true;
    }

    boolean owns(int containerId) { return ownedId >= 0 && ownedId == containerId; }
    boolean isCandidate(int containerId) { return candidateId >= 0 && candidateId == containerId; }

    void reset() {
        expected = null;
        candidateId = -1;
        ownedId = -1;
    }
}
