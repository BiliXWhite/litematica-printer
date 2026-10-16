//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.*;
import net.minecraft.network.HashedStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Characterizes vanilla's actual synchronization contract, without launching a game client. */
class QuickShulkerPacketContractTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }
    static <T> T allocate(Class<T> type) throws Exception {
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)f.get(null)).allocateInstance(type));
    }
    static class AuditLevel extends net.minecraft.client.multiplayer.ClientLevel {
        AuditLevel() { super(null,null,null,null,0,0,null,false,0L,0); }
        @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { return net.minecraft.world.flag.FeatureFlags.VANILLA_SET; }
    }
    static class AuditPlayer extends net.minecraft.world.entity.player.Player {
        net.minecraft.world.entity.player.Inventory inventoryForAudit;
        AuditLevel levelForAudit;
        AuditPlayer() { super(null,null); }
        @Override public net.minecraft.world.entity.player.Inventory getInventory() { return inventoryForAudit; }
        @Override public net.minecraft.world.level.Level level() { return levelForAudit; }
        @Override public net.minecraft.world.level.GameType gameMode() { return net.minecraft.world.level.GameType.SURVIVAL; }
    }

    static class ServerMenu extends AbstractContainerMenu {
        ServerMenu(net.minecraft.world.Container box, net.minecraft.world.entity.player.Inventory inventory) {
            super(null, 7);
            addSlot(new Slot(box, 0, 0, 0));
            addSlot(new Slot(inventory, 2, 0, 0));
        }
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    }
    static class Replies implements ContainerSynchronizer {
        final Map<Integer, ItemStack> slots = new HashMap<>();
        int fullSnapshots;
        @Override public void sendInitialData(AbstractContainerMenu menu, List<ItemStack> items, ItemStack carried, int[] data) { fullSnapshots++; }
        @Override public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) { slots.put(slot, stack.copy()); }
        @Override public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) { }
        @Override public void sendDataChange(AbstractContainerMenu menu, int slot, int value) { }
        @Override public RemoteSlot createSlot() { return new RemoteSlot.Synchronized(component -> component.hashCode()); }
    }
    @Test void swapWithoutPredictedSlotClaimsProducesBothServerAcknowledgements() throws Exception {
        var player = allocate(AuditPlayer.class);
        player.levelForAudit = allocate(AuditLevel.class);
        player.inventoryForAudit = new net.minecraft.world.entity.player.Inventory(player, new net.minecraft.world.entity.EntityEquipment());
        var box = new net.minecraft.world.SimpleContainer(1);
        box.setItem(0, new ItemStack(Items.STONE, 64));
        var menu = new ServerMenu(box, player.inventoryForAudit);
        var replies = new Replies();
        menu.setSynchronizer(replies);
        menu.suppressRemoteUpdates();
        menu.clicked(0, 2, ContainerInput.SWAP, player);
        // QuickShulkerClicks sends no predicted changedSlots and the pre-click cursor snapshot.
        menu.setRemoteCarried(HashedStack.EMPTY);
        menu.resumeRemoteUpdates();
        menu.broadcastChanges();
        assertEquals(Set.of(0, 1), replies.slots.keySet());
        assertTrue(replies.slots.get(0).isEmpty());
        assertEquals(64, replies.slots.get(1).getCount());
        assertTrue(replies.slots.get(1).is(Items.STONE));
    }

    @Test void cancellationSyncClickDoesNotMoveOrDropACarriedStack() throws Exception {
        var player = allocate(AuditPlayer.class);
        player.levelForAudit = allocate(AuditLevel.class);
        player.inventoryForAudit = new net.minecraft.world.entity.player.Inventory(player, new net.minecraft.world.entity.EntityEquipment());
        var box = new net.minecraft.world.SimpleContainer(1);
        box.setItem(0, new ItemStack(Items.STONE, 64));
        var menu = new ServerMenu(box, player.inventoryForAudit);
        var replies = new Replies();
        menu.setSynchronizer(replies);
        menu.setCarried(new ItemStack(Items.SHULKER_BOX));
        int before = replies.fullSnapshots;
        assertTrue(menu.isValidSlotIndex(-1));
        assertNotEquals(-1, menu.getStateId());
        menu.suppressRemoteUpdates();
        menu.clicked(-1, 0, ContainerInput.PICKUP, player);
        menu.resumeRemoteUpdates();
        // The actual server handler takes this branch for the impossible requested revision -1.
        menu.broadcastFullState();
        assertEquals(before + 1, replies.fullSnapshots);
        assertEquals(64, box.getItem(0).getCount());
        assertTrue(player.inventoryForAudit.getItem(2).isEmpty());
        assertTrue(menu.getCarried().is(Items.SHULKER_BOX));
    }
}
//#endif
