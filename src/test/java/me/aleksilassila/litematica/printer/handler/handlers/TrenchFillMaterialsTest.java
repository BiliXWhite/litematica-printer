package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.malilib.config.options.ConfigStringList;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the oldest, current and newest Minecraft registries. */
class TrenchFillMaterialsTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        //#if MC >= 260100
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                //#if MC >= 260300
                //$$ .build(net.minecraft.data.registries.VanillaRegistries.createWorldLookup()).forEach(p -> p.apply());
                //#else
                .build(net.minecraft.data.registries.VanillaRegistries.createLookup()).forEach(p -> p.apply());
                //#endif
        //#endif
    }

    @Test void configuredBlocksReplaceSandAndInvalidListsStayEmpty() throws Exception {
        var config = (ConfigStringList) Configs.Mine.class.getField("MINE_TRENCH_FILL_BLOCK_LIST").get(null);
        var original = List.copyOf(config.getStrings());
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var handler = (MineHandler) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(MineHandler.class);
        var refresh = MineHandler.class.getDeclaredMethod("refreshTrenchFillMaterials");
        refresh.setAccessible(true);
        var items = MineHandler.class.getDeclaredField("trenchFillItems");
        items.setAccessible(true);
        try {
            assertTrue(Configs.Mine.OPTIONS.contains(config));
            config.resetToDefault();
            refresh.invoke(handler);
            assertArrayEquals(new Item[]{Items.SAND}, (Item[]) items.get(handler));

            config.setStrings(List.of("minecraft:cobblestone"));
            refresh.invoke(handler);
            assertArrayEquals(new Item[]{Items.COBBLESTONE}, (Item[]) items.get(handler));

            config.setStrings(List.of("minecraft:cobblestone", "minecraft:stone", "minecraft:cobblestone"));
            refresh.invoke(handler);
            assertArrayEquals(new Item[]{Items.COBBLESTONE, Items.STONE}, (Item[]) items.get(handler));

            for (var invalid : List.of(List.<String>of(), List.of("minecraft:air"),
                    List.of("minecraft:stick"), List.of("minecraft:nonexistent_trench_material"))) {
                config.setStrings(invalid);
                refresh.invoke(handler);
                assertEquals(0, ((Item[]) items.get(handler)).length, "Never fall back to sand: " + invalid);
            }
        } finally {
            config.setStrings(original);
        }
    }
}
