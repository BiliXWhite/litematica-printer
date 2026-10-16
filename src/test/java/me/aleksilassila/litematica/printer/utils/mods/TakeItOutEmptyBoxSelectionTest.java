package me.aleksilassila.litematica.printer.utils.mods;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import me.aleksilassila.litematica.printer.utils.minecraft.StringUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

public class TakeItOutEmptyBoxSelectionTest {
    public record Entry(ItemStack stack, int count) {}
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    private static ItemStack full() {
        var box = new ItemStack(Items.SHULKER_BOX);
        //#if MC >= 12006
        box.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
        //#else
        //$$ var stored = new ItemStack(Items.DIAMOND, 64).save(new net.minecraft.nbt.CompoundTag());
        //$$ stored.putByte("Slot", (byte) 0);
        //$$ var items = new net.minecraft.nbt.ListTag();
        //$$ items.add(stored);
        //$$ box.getOrCreateTagElement("BlockEntityTag").put("Items", items);
        //#endif
        return box;
    }

    private static ItemStack namedEmpty() {
        var box = new ItemStack(Items.SHULKER_BOX);
        //#if MC >= 12006
        box.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, StringUtils.literal("empty box"));
        //#else
        //$$ box.setHoverName(StringUtils.literal("empty box"));
        //#endif
        return box;
    }

    @ParameterizedTest @ValueSource(booleans={false, true})
    void choosesTheEmptyVariantRegardlessOfFilledBoxOrder(boolean fullFirst) throws Exception {
        var empty = namedEmpty();
        var filled = new Entry(full(), 1);
        var available = new Entry(empty, 3);
        var entries = fullFirst ? List.of(filled, available) : List.of(available, filled);
        var selected = TakeItOutUtils.selectEmptyShulkerRequest(new ItemStack(Items.SHULKER_BOX), entries);
        assertEquals(empty.getHoverName(), selected.getHoverName());
        assertTrue(me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest.isEmptyShulker(selected));
        assertNotSame(empty, selected);
        assertEquals(1, selected.getCount());
        selected.setCount(0);
        assertEquals(1, empty.getCount(), "The dependency's material cache must not be mutated");
    }

    @Test void absentOrStaleEmptyEntriesNeverCauseAFilledBoxRequest() throws Exception {
        var required = new ItemStack(Items.SHULKER_BOX);
        var selected = TakeItOutUtils.selectEmptyShulkerRequest(required,
                List.of(new Entry(full(), 8), new Entry(namedEmpty(), 0), new Entry(new ItemStack(Items.DIAMOND), 4)));
        assertTrue(me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest.isEmptyShulker(selected));
        assertEquals(required.getHoverName(), selected.getHoverName());
        assertNotSame(required, selected);
        assertTrue(me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest.isEmptyShulker(
                TakeItOutUtils.selectEmptyShulkerRequest(required, List.of())));
    }
}
