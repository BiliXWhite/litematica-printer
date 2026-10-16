//#if MC == 260100
package me.aleksilassila.litematica.printer.integration.quickshulker;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import me.aleksilassila.litematica.printer.integration.inventory.*;
import me.aleksilassila.litematica.printer.runtime.*;
import me.aleksilassila.litematica.printer.utils.*;
import me.aleksilassila.litematica.printer.guide.DefaultGuide;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class EmptyShulkerRetrievalTest {
    @BeforeAll static void bootstrap() throws Exception { CancelledSwapLifecycleTest.bootstrap(); }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void missingEmptyBoxReachesExternalProviderEvenWithFilledBoxInInventory(boolean carryFilledBox) throws Exception {
        try (var f = new CancelledSwapLifecycleTest.Fixture()) {
            CancelledSwapLifecycleTest.set(f.player, Player.class, "abilities", new Abilities());
            CancelledSwapLifecycleTest.set(f.player, Player.class, "inventory", f.player.inventory);
            CancelledSwapLifecycleTest.set(f.player, net.minecraft.world.entity.LivingEntity.class, "equipment", new net.minecraft.world.entity.EntityEquipment());
            f.player.containerMenu = f.player.inventoryMenu;
            ItemStack full = new ItemStack(Items.SHULKER_BOX);
            full.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,64))));
            if (carryFilledBox) f.player.inventory.setItem(10, full);
            var seen = new AtomicReference<MaterialRequest>();
            InventoryProvider external = new InventoryProvider() {
                public String id() { return "external"; }
                public MaterialReservation request(MaterialRequest r) { seen.set(r); return new MaterialReservation(r.token(), MaterialReservation.State.PENDING); }
                public MaterialReservation status(MaterialRequest r) { return new MaterialReservation(r.token(), MaterialReservation.State.PENDING); }
            };
            var coordinator = new MaterialRequestCoordinator(List.of(new PlayerInventoryProvider(f.client), external));
            CancelledSwapLifecycleTest.set(RuntimeAccess.get(), PrinterRuntime.class, "materialRequests", coordinator);
            CancelledSwapLifecycleTest.set(RuntimeAccess.get(), PrinterRuntime.class, "inventorySwitchGuard", new InventorySwitchGuard(f.client, () -> 0L));
            var action = InventoryRegressionTest.missing(new DefaultGuide(InventoryRegressionTest.context(Blocks.SHULKER_BOX.defaultBlockState()))).action();
            assertFalse(MaterialSelector.switchToMatchingStack(f.player, action.getRequiredStackPredicate(), action.getRequiredCreativeStack(), -1));
            assertNotNull(seen.get(), "The existing provider chain must receive the missing empty-box request");
            assertTrue(seen.get().emptyShulkerOnly());
            assertFalse(seen.get().acceptsStack(full));
            assertTrue(seen.get().acceptsStack(new ItemStack(Items.SHULKER_BOX)));
            assertFalse(action.getRequiredStackPredicate().test(full), "Retrieval must not weaken placement protection");
            assertTrue(action.getRequiredStackPredicate().test(new ItemStack(Items.SHULKER_BOX)));
        }
    }
}
//#endif
