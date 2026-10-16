package me.aleksilassila.litematica.printer.integration.quickshulker;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class QuickShulkerContainerSessionTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }

    @Test void sameTypeManualContainerIsNotOwnedBeforeMatchingInitialContents() {
        var session = new QuickShulkerContainerSession();
        session.begin(new ItemStack(Items.SHULKER_BOX), true);
        session.opened(7, MenuType.GENERIC_9x3);
        assertFalse(session.owns(7));
        var unrelated = new ArrayList<>(Collections.nCopies(63, ItemStack.EMPTY));
        unrelated.set(0, new ItemStack(Items.DIRT, 64));
        assertFalse(session.confirm(7, unrelated, ItemStack.EMPTY));
        assertFalse(session.owns(7));
        session.opened(8, MenuType.GENERIC_9x3);
        assertTrue(session.confirm(8, Collections.nCopies(63, ItemStack.EMPTY), ItemStack.EMPTY));
        assertTrue(session.owns(8));
        assertFalse(session.owns(7));
    }

    @Test void wrongTypeCursorAndLateContentCannotAuthorizeTransfers() {
        var session = new QuickShulkerContainerSession();
        session.begin(new ItemStack(Items.SHULKER_BOX), false);
        session.opened(7, MenuType.GENERIC_9x3);
        assertFalse(session.confirm(7, Collections.nCopies(63, ItemStack.EMPTY), ItemStack.EMPTY));
        session.opened(8, MenuType.SHULKER_BOX);
        assertFalse(session.confirm(8, Collections.nCopies(63, ItemStack.EMPTY), new ItemStack(Items.DIRT)));
        session.reset();
        assertFalse(session.confirm(8, Collections.nCopies(63, ItemStack.EMPTY), ItemStack.EMPTY));
    }

    @Test void cancelledRestoreCannotHideOrClaimALateContainer() throws Exception {
        var controller = new QuickShulkerRequestController(null);
        var field = QuickShulkerRequestController.class.getDeclaredField("containerSession");
        field.setAccessible(true);
        var session = (QuickShulkerContainerSession) field.get(controller);
        session.begin(new ItemStack(Items.SHULKER_BOX), false);
        session.opened(7, MenuType.SHULKER_BOX);
        // Neither extraction nor restore is pending any more. Never proceed to client UI operations.
        boolean accepted = assertDoesNotThrow(() -> controller.confirmContainer(
                7, Collections.nCopies(63, ItemStack.EMPTY), ItemStack.EMPTY));
        assertFalse(accepted);
        assertFalse(session.owns(7));
    }
}
