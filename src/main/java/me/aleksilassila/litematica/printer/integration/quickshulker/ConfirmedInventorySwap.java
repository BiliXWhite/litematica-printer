package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.item.ItemStack;

/** Only packet payloads may acknowledge this swap; client prediction is not evidence. */
final class ConfirmedInventorySwap {
    private int containerId = -1;
    private int sourceSlot;
    private int targetSlot;
    private ItemStack sourceAfter = ItemStack.EMPTY;
    private ItemStack targetAfter = ItemStack.EMPTY;
    private boolean sourceConfirmed;
    private boolean targetConfirmed;

    void begin(int containerId, int sourceSlot, int targetSlot, ItemStack sourceBefore, ItemStack targetBefore) {
        this.containerId = containerId;
        this.sourceSlot = sourceSlot;
        this.targetSlot = targetSlot;
        this.sourceAfter = targetBefore.copy();
        this.targetAfter = sourceBefore.copy();
        this.sourceConfirmed = false;
        this.targetConfirmed = false;
    }

    void observe(int containerId, int slot, ItemStack stack) {
        if (!active() || this.containerId != containerId) return;
        if (slot == this.sourceSlot) this.sourceConfirmed = ItemStack.matches(this.sourceAfter, stack);
        if (slot == this.targetSlot) this.targetConfirmed = ItemStack.matches(this.targetAfter, stack);
    }

    boolean active() { return this.containerId >= 0; }
    boolean confirmed() { return active() && this.sourceConfirmed && this.targetConfirmed; }

    void reset() {
        this.containerId = -1;
        this.sourceAfter = ItemStack.EMPTY;
        this.targetAfter = ItemStack.EMPTY;
        this.sourceConfirmed = false;
        this.targetConfirmed = false;
    }
}
