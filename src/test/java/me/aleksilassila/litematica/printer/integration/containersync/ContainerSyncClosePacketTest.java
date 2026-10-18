//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.containersync;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.List;
import static me.aleksilassila.litematica.printer.integration.containersync.ContainerSyncControllerTest.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real packet construction and vanilla cursor cleanup, without a running game/server. */
class ContainerSyncClosePacketTest {
    @BeforeAll static void bootstrap() { ContainerSyncPlanTest.bootstrap(); }

    static class Connection extends ClientPacketListener {
        List<Packet<?>> packets;
        Connection() { super(null, null, null); }
        @Override public void send(Packet<?> packet) { packets.add(packet); }
    }

    @Test void forcedCloseRequestsOnlyOneNonMutatingSnapshotWhileDisabled() throws Exception {
        try (var f = new Fixture()) {
            var connection = allocate(Connection.class);
            connection.packets = new ArrayList<>();
            set(f.client.player, LocalPlayer.class, "connection", connection);
            set(f.controller, ContainerSyncController.class, "connection", connection);
            var clicks = f.startTransfer("insertion").clicks;
            var inventory = new Menu(0);
            f.client.player.containerMenu = inventory;
            f.controller.tick();
            me.aleksilassila.litematica.printer.config.Configs.Core.WORK_SWITCH.setBooleanValue(false);
            for (int tick = 0; tick < 200; tick++) f.controller.tick();
            assertFalse(f.otherInventoryOwner());
            assertEquals(List.of(1, 0), clicks);
            assertEquals(1, connection.packets.size());
            var request = assertInstanceOf(ServerboundContainerClickPacket.class, connection.packets.get(0));
            assertEquals(0, request.containerId());
            assertEquals(-1, request.stateId());
            assertEquals(-1, request.slotNum()); // -999 would drop; -1 is a vanilla no-op.
            assertEquals(ContainerInput.PICKUP, request.containerInput());
            assertEquals(net.minecraft.network.HashedStack.EMPTY, request.carriedItem(), "Never copy the old menu's cursor");
            assertTrue(request.changedSlots().isEmpty());
            f.controller.content(0, 1, List.of(), ItemStack.EMPTY);
            assertTrue(f.otherInventoryOwner());
            assertEquals(1, connection.packets.size(), "Do not close the replacement menu or send a cleanup click");
            assertSame(inventory, f.client.player.containerMenu);
        }
    }

    static class ServerPlayer extends net.minecraft.server.level.ServerPlayer {
        Inventory inventory;
        List<ItemStack> dropped;
        ServerPlayer() { super(null, null, null, null); }
        @Override public Inventory getInventory() { return inventory; }
        @Override public boolean hasDisconnected() { return false; }
        @Override public ItemEntity drop(ItemStack stack, boolean random) {
            dropped.add(stack.copy());
            return null;
        }
    }

    static class ServerInventory extends Inventory {
        ServerInventory(ServerPlayer player) { super(player, new EntityEquipment()); }
        @Override public void placeItemBackInInventory(ItemStack stack) {
            // Exercise vanilla placement/drop behavior without needing a live connection for slot notifications.
            super.placeItemBackInInventory(stack, false);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"space", "partial", "full"})
    void vanillaForcedCloseReturnsCursorOrDropsOnlyWhatCannotFit(String capacity) throws Exception {
        var player = allocate(ServerPlayer.class);
        player.inventory = new ServerInventory(player);
        player.dropped = new ArrayList<>();
        set(player, net.minecraft.world.entity.player.Player.class, "abilities", new Abilities());
        if (!capacity.equals("space")) {
            for (int slot = 0; slot < 36; slot++) player.inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
            if (capacity.equals("partial")) player.inventory.setItem(0, new ItemStack(Items.STONE, 63));
        }
        var menu = new Menu(7);
        menu.setCarried(new ItemStack(Items.STONE, 4));
        menu.removed(player); // The actual server close path invokes this before switching to inventoryMenu.
        assertTrue(menu.getCarried().isEmpty());
        if (capacity.equals("space")) {
            assertEquals(4, player.inventory.getItem(0).getCount());
            assertTrue(player.inventory.getItem(0).is(Items.STONE));
            assertTrue(player.dropped.isEmpty());
        } else {
            assertEquals(1, player.dropped.size());
            assertEquals(capacity.equals("partial") ? 3 : 4, player.dropped.get(0).getCount());
            assertTrue(player.dropped.get(0).is(Items.STONE));
            if (capacity.equals("partial")) assertEquals(64, player.inventory.getItem(0).getCount());
        }
    }
}
//#endif
