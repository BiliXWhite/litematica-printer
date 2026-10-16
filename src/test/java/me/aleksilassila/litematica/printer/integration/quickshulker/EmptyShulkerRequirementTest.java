package me.aleksilassila.litematica.printer.integration.quickshulker;

import me.aleksilassila.litematica.printer.integration.inventory.*;
import net.minecraft.world.item.*;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmptyShulkerRequirementTest {
    @BeforeAll static void bootstrap() { InventoryRegressionTest.bootstrap(); }
    @Test void legacyAndComponentBoxesKeepTheSameEmptyRequirementThroughChestMatching() throws Exception {
        var full = new ItemStack(Items.SHULKER_BOX);
        //#if MC >= 12006
        full.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,64))));
        //#else
        //$$ var stored = new ItemStack(Items.DIAMOND,64).save(new net.minecraft.nbt.CompoundTag());
        //$$ stored.putByte("Slot", (byte) 0);
        //$$ var items = new net.minecraft.nbt.ListTag();
        //$$ items.add(stored);
        //$$ full.getOrCreateTagElement("BlockEntityTag").put("Items", items);
        //#endif
        var request = new MaterialRequest(1, List.of(Items.SHULKER_BOX), Items.SHULKER_BOX, 1, MaterialRequest.Source.PRINT, true);
        assertFalse(request.acceptsStack(full));
        assertTrue(request.acceptsStack(new ItemStack(Items.SHULKER_BOX)));
        //#if MC >= 260200
        //$$ assertFalse(request.acceptsStack(new ItemStack(Items.DYED_SHULKER_BOX.red())));
        //#else
        assertFalse(request.acceptsStack(new ItemStack(Items.RED_SHULKER_BOX)));
        //#endif
        assertFalse(request.acceptsStack(ItemStack.EMPTY));
        var normal = MaterialRequest.single(2, Items.SHULKER_BOX, 1, MaterialRequest.Source.PICK_BLOCK);
        assertTrue(normal.acceptsStack(full), "Ordinary pick-block keeps its existing semantics");
        var chest = new ChestTrackerAdapter(null);
        InventoryRegressionTest.set(chest, "activeRequest", request);
        InventoryRegressionTest.set(chest, "requestedItems", List.of(Items.SHULKER_BOX));
        var matches = ChestTrackerAdapter.class.getDeclaredMethod("matches", ItemStack.class);
        matches.setAccessible(true);
        assertFalse((boolean) matches.invoke(chest, full));
        assertTrue((boolean) matches.invoke(chest, new ItemStack(Items.SHULKER_BOX)));
        InventoryRegressionTest.set(chest, "activeRequest", normal);
        assertTrue((boolean) matches.invoke(chest, full));
    }
}
