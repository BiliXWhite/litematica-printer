package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.item.ItemStack;

/** Owns only the cursor stack originating from one server-plugin open attempt. */
public final class ShulkerCursorRecovery {
    private int slot = -1;
    private ItemStack stack = ItemStack.EMPTY;
    private boolean failed;
    private boolean returnSent;
    private boolean slotConfirmed;
    private boolean cursorConfirmed;

    public void begin(int slot, ItemStack stack) {
        reset();
        this.slot = slot;
        this.stack = stack.copy();
    }

    public void fail() { if (slot >= 0) failed = true; }
    public boolean active() { return failed; }
    public int slot() { return slot; }

    public boolean shouldReturn(ItemStack carried, ItemStack originalSlot) {
        return failed && !returnSent && QuickShulkerSupport.canRecoverCursor(stack, carried, originalSlot);
    }

    public void markReturnSent() {
        returnSent = true;
        slotConfirmed = false;
        cursorConfirmed = false;
    }

    public void observe(int containerId, int slot, ItemStack received) {
        if (this.slot < 0) return;
        if (containerId == 0 && slot == this.slot) slotConfirmed = ItemStack.matches(stack, received);
        if (containerId == -1 && slot == -1) cursorConfirmed = received.isEmpty();
    }

    public boolean confirmed() { return failed && returnSent && slotConfirmed && cursorConfirmed; }

    public void reset() {
        slot = -1;
        stack = ItemStack.EMPTY;
        failed = false;
        returnSent = false;
        slotConfirmed = false;
        cursorConfirmed = false;
    }
}
