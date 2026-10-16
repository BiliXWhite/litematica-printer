package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShulkerCursorRecoveryTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }

    @Test void latePickupAfterOpenTimeoutIsReturnedOnlyOnceAndWaitsForServer() {
        var recovery = new ShulkerCursorRecovery();
        var box = new ItemStack(Items.SHULKER_BOX);
        recovery.begin(12, box);
        recovery.fail();
        assertFalse(recovery.confirmed());
        assertFalse(recovery.shouldReturn(ItemStack.EMPTY, box));
        assertTrue(recovery.shouldReturn(box, ItemStack.EMPTY));
        recovery.markReturnSent();
        assertFalse(recovery.shouldReturn(box, ItemStack.EMPTY));
        recovery.observe(0, 12, box);
        assertFalse(recovery.confirmed());
        recovery.observe(-1, -1, ItemStack.EMPTY);
        assertTrue(recovery.confirmed());
    }

    @Test void resetPreventsReturningItemsFromAnOldWorldOrManualCursor() {
        var recovery = new ShulkerCursorRecovery();
        var box = new ItemStack(Items.SHULKER_BOX);
        recovery.begin(12, box);
        recovery.fail();
        assertFalse(recovery.shouldReturn(new ItemStack(Items.DIRT), ItemStack.EMPTY));
        recovery.reset();
        assertFalse(recovery.shouldReturn(box, ItemStack.EMPTY));
    }

    @Test void inventoryRepliesFromBeforeThePickupCannotConfirmAReturn() {
        var recovery = new ShulkerCursorRecovery();
        var box = new ItemStack(Items.SHULKER_BOX);
        recovery.begin(12, box);
        recovery.observe(0, 12, box);
        recovery.observe(-1, -1, ItemStack.EMPTY);
        recovery.fail();
        assertFalse(recovery.confirmed());
        assertTrue(recovery.shouldReturn(box, ItemStack.EMPTY));
    }

    @Test void workSwitchResetRetainsCursorOwnershipButChangingServerDiscardsIt() throws Exception {
        var utility = me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils.class;
        var field = utility.getDeclaredField("recovery");
        field.setAccessible(true);
        var recovery = (ShulkerCursorRecovery) field.get(null);
        var box = new ItemStack(Items.SHULKER_BOX);
        try {
            recovery.begin(12, box);
            me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils.onRuntimeReset("work_switch_off");
            recovery.fail();
            assertTrue(recovery.shouldReturn(box, ItemStack.EMPTY));
            me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils.onRuntimeReset("client_scope_changed");
            recovery.fail();
            assertFalse(recovery.shouldReturn(box, ItemStack.EMPTY));
        } finally {
            recovery.reset();
        }
    }

    @Test void disabledPrinterStillTicksPendingCursorCleanup() throws Exception {
        var work = me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH;
        var quick = me.aleksilassila.litematica.printer.config.Configs.Placement.QUICK_SHULKER;
        boolean oldWork = work.getBooleanValue(), oldQuick = quick.getBooleanValue();
        var field = me.aleksilassila.litematica.printer.utils.mods.ShulkerUtils.class.getDeclaredField("recovery");
        field.setAccessible(true);
        var recovery = (ShulkerCursorRecovery) field.get(null);
        var runtimeField = me.aleksilassila.litematica.printer.runtime.RuntimeAccess.class.getDeclaredField("current");
        runtimeField.setAccessible(true);
        Object previousRuntime = runtimeField.get(null);
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var runtime = (me.aleksilassila.litematica.printer.runtime.PrinterRuntime)
                ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(me.aleksilassila.litematica.printer.runtime.PrinterRuntime.class);
        InventoryRegressionTest.set(runtime, "quickShulkerAdapter", new QuickShulkerAdapter(null));
        try {
            me.aleksilassila.litematica.printer.runtime.RuntimeAccess.install(runtime);
            recovery.begin(12, new ItemStack(Items.SHULKER_BOX));
            recovery.fail();
            work.setBooleanValue(false);
            quick.setBooleanValue(false);
            // With no client/player, cleanup must discard the old cursor ownership even while disabled.
            me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge.onTick();
            assertFalse(recovery.active());
        } finally {
            runtimeField.set(null, previousRuntime);
            recovery.reset();
            work.setBooleanValue(oldWork);
            quick.setBooleanValue(oldQuick);
        }
    }
}
