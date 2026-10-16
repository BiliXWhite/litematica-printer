package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmedInventorySwapTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }

    @Test void waitsForBothServerSlotsAndIgnoresOtherContainers() {
        var swap = new ConfirmedInventorySwap();
        swap.begin(7, 2, 30, new ItemStack(Items.STONE, 64), ItemStack.EMPTY);
        assertFalse(swap.confirmed());
        swap.observe(8, 2, ItemStack.EMPTY);
        swap.observe(7, 30, new ItemStack(Items.STONE, 64));
        assertFalse(swap.confirmed());
        swap.observe(7, 2, ItemStack.EMPTY);
        assertTrue(swap.confirmed());
    }

    @Test void rejectionOrPartialCountDoesNotConfirmAndResetDropsLatePackets() {
        var swap = new ConfirmedInventorySwap();
        swap.begin(7, 2, 30, new ItemStack(Items.STONE, 64), ItemStack.EMPTY);
        swap.observe(7, 2, new ItemStack(Items.STONE, 64));
        swap.observe(7, 30, new ItemStack(Items.STONE, 63));
        assertFalse(swap.confirmed());
        swap.observe(7, 2, ItemStack.EMPTY);
        assertFalse(swap.confirmed());
        swap.reset();
        swap.observe(7, 30, new ItemStack(Items.STONE, 64));
        assertFalse(swap.confirmed());
    }

    @Test void finalHotbarSelectionRejectsSourceBoxAndOutOfRangeSlots() {
        assertFalse(QuickShulkerSupport.isSafeHotbarTarget(8, 44, new ItemStack(Items.SHULKER_BOX)));
        assertFalse(QuickShulkerSupport.isSafeHotbarTarget(8, 44, ItemStack.EMPTY));
        assertFalse(QuickShulkerSupport.isSafeHotbarTarget(9, 12, ItemStack.EMPTY));
        assertTrue(QuickShulkerSupport.isSafeHotbarTarget(2, 12, ItemStack.EMPTY));
    }

    @Test void cursorRecoveryRequiresExactOwnedStackAndEmptyOriginalSlot() {
        var box = new ItemStack(Items.SHULKER_BOX);
        assertTrue(QuickShulkerSupport.canRecoverCursor(box, box.copy(), ItemStack.EMPTY));
        assertFalse(QuickShulkerSupport.canRecoverCursor(box, new ItemStack(Items.DIRT), ItemStack.EMPTY));
        assertFalse(QuickShulkerSupport.canRecoverCursor(box, box.copy(), new ItemStack(Items.DIRT)));
        assertFalse(QuickShulkerSupport.canRecoverCursor(ItemStack.EMPTY, box, ItemStack.EMPTY));
    }
}
