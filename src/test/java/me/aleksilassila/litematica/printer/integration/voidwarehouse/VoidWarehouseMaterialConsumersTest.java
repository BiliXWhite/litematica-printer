//#if MC >= 12111
package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.MixinMaterialListHudRenderer;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.MixinWidgetMaterialListEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VoidWarehouseMaterialConsumersTest {
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

    @AfterEach void clearCache() { VoidWarehouseMaterialCache.get().clear(); }

    @Test void hugeWarehouseWithPositiveInventoryDoesNotReappearAsMissing() throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of("minecraft:stone"), List.of(Long.MAX_VALUE));
        var loader = new MaterialListTestLoader();
        var entry = entry(loader, 100, 100, 0, 64);
        var list = list(loader, entry, 2);
        assertEquals(Integer.MAX_VALUE, entry.getClass().getMethod("getCountAvailable").invoke(entry));
        list.getClass().getMethod("recreateFilteredList").invoke(list);
        assertEquals(0, ((List<?>) field(list, "materialListFiltered", null)).size());
    }

    @Test void equalCollectionGapsKeepNativeNameTieBreaking() throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of(), List.of());
        var loader = new MaterialListTestLoader();
        var first = entry(loader, 100, 100, 0, 60);
        var second = entry(loader, 80, 80, 0, 40);
        var list = list(loader, first, 1);
        var criteria = loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase$SortCriteria");
        field(list, "sortCriteria", criteria.getField("COUNT_MISSING").get(null));
        var sorter = loader.loadClass("fi.dy.masa.litematica.materials.MaterialListSorter")
                .getConstructor(loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase")).newInstance(list);
        var compare = sorter.getClass().getMethod("compare", first.getClass(), first.getClass());
        assertEquals(0, compare.invoke(sorter, first, second));
        field(list, "reverse", true);
        assertEquals(0, compare.invoke(sorter, first, second));
    }

    @ParameterizedTest @CsvSource({
            "1,0,0,60", "1,20,20,20", "1,20,40,0", "1,20,100,0",
            "2,20,20,88", "2,20,107,1", "2,20,108,0", "2,20,200,0",
            "1,64,9223372036854775807,0", "2,64,9223372036854775807,0"
    })
    void hoverDetailsShowCollectionGapAndFallBackAfterCacheClear(int multiplier, int inventory, long warehouse,
                                                                int gap) throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of("minecraft:stone"), List.of(warehouse));
        var loader = new MaterialListTestLoader();
        var entry = entry(loader, 64, 60, 20, inventory);
        var list = list(loader, entry, multiplier);
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        var widget = ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(
                loader.loadClass("fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry"));
        field(widget, "entry", entry);
        field(widget, "materialList", list);
        var hover = widget.getClass().getMethod("postRenderHovered");
        assertArrayEquals(new int[] {64 * multiplier, gap}, (int[]) hover.invoke(widget));
        assertEquals(60, entry.getClass().getMethod("getCountMissing").invoke(entry), "World demand stays unchanged");
        VoidWarehouseMaterialCache.get().clear();
        assertArrayEquals(new int[] {64 * multiplier, multiplier == 1 ? 60 : 128}, (int[]) hover.invoke(widget));
    }

    @ParameterizedTest @CsvSource({
            "1,100,100,80,80,1", "2,100,100,80,80,1",
            "1,100,20,60,60,1", "2,100,20,60,60,-1"
    })
    void missingSortUsesDisplayedGapWithMultiplierAndBothDirections(int multiplier, int total, int missing,
                                                                   int otherTotal, int otherMissing, int sign) throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of("minecraft:stone"), List.of(60L));
        var loader = new MaterialListTestLoader();
        var first = entry(loader, total, missing, 0, 0);
        var second = first.getClass().getConstructor(ItemStack.class, int.class, int.class, int.class, int.class)
                .newInstance(new ItemStack(Items.DIRT), otherTotal, otherMissing, 0, 0);
        var list = list(loader, first, multiplier);
        var criteria = loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase$SortCriteria");
        field(list, "sortCriteria", criteria.getField("COUNT_MISSING").get(null));
        var sorter = loader.loadClass("fi.dy.masa.litematica.materials.MaterialListSorter")
                .getConstructor(loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase")).newInstance(list);
        var compare = sorter.getClass().getMethod("compare", first.getClass(), first.getClass());
        assertEquals(sign, Integer.signum((int) compare.invoke(sorter, first, second)));
        assertEquals(-sign, Integer.signum((int) compare.invoke(sorter, second, first)));
        field(list, "reverse", true);
        assertEquals(-sign, Integer.signum((int) compare.invoke(sorter, first, second)));
        field(list, "reverse", false);
        VoidWarehouseMaterialCache.get().clear();
        assertEquals(missing > otherMissing ? -1 : 1, Integer.signum((int) compare.invoke(sorter, first, second)));
    }

    @ParameterizedTest @ValueSource(strings = {"COUNT_TOTAL", "COUNT_AVAILABLE", "NAME"})
    void otherSortCriteriaRetainNativeOrdering(String sort) throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of(), List.of());
        var loader = new MaterialListTestLoader();
        var first = entry(loader, 100, 100, 0, 60);
        var second = entry(loader, 80, 80, 0, 0);
        var list = list(loader, first, 2);
        var criteria = loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase$SortCriteria");
        field(list, "sortCriteria", criteria.getField(sort).get(null));
        var sorter = loader.loadClass("fi.dy.masa.litematica.materials.MaterialListSorter")
                .getConstructor(loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase")).newInstance(list);
        var compare = sorter.getClass().getMethod("compare", first.getClass(), first.getClass());
        assertEquals(sort.equals("NAME") ? 0 : -1, compare.invoke(sorter, first, second));
    }

    static Object field(Object instance, String name, Object value) throws Exception {
        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                var field = type.getDeclaredField(name); field.setAccessible(true);
                if (value != null) field.set(instance, value);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    static Object entry(ClassLoader loader, int total, int missing, int mismatched, int available) throws Exception {
        return loader.loadClass("fi.dy.masa.litematica.materials.MaterialListEntry")
                .getConstructor(ItemStack.class, int.class, int.class, int.class, int.class)
                .newInstance(new ItemStack(Items.STONE), total, missing, mismatched, available);
    }

    static Object list(ClassLoader loader, Object entry, int multiplier) throws Exception {
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        var result = unsafe.allocateInstance(loader.loadClass("fi.dy.masa.litematica.materials.MaterialListSchematic"));
        field(result, "materialListAll", ImmutableList.of(entry));
        field(result, "materialListPreFiltered", new ArrayList<>(List.of(entry)));
        field(result, "materialListFiltered", new ArrayList<>());
        field(result, "multiplier", multiplier);
        return result;
    }

    @ParameterizedTest @CsvSource({"128,0,false", "64,64,false", "63,64,true", "0,128,false"})
    void multipliedDemandUsesCombinedStockWhenFiltering(int inventory, long warehouse, boolean missing) throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of("minecraft:stone"), List.of(warehouse));
        var loader = new MaterialListTestLoader();
        var list = list(loader, entry(loader, 64, 64, 0, inventory), 2);
        list.getClass().getMethod("recreateFilteredList").invoke(list);
        assertEquals(missing ? 1 : 0, ((List<?>) field(list, "materialListFiltered", null)).size());
    }

    @Test void collectedMaterialsDoNotChangeWorldProgressOrProduceNegativeMissingCounts() throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of("minecraft:stone"), List.of(100L));
        var loader = new MaterialListTestLoader();
        var entry = entry(loader, 100, 60, 20, 0);
        var list = list(loader, entry, 1);
        list.getClass().getMethod("updateCounts").invoke(list);
        long missing = (long) list.getClass().getMethod("getCountMissing").invoke(list);
        long mismatched = (long) list.getClass().getMethod("getCountMismatched").invoke(list);
        assertEquals(60, missing, "World demand includes 40 absent blocks and 20 wrong blocks");
        assertEquals(40, missing - mismatched, "GuiMaterialList subtracts mismatched blocks from world demand");
        assertEquals(40, 100 - missing, "Stock does not finish building the schematic");
        assertEquals(100, entry.getClass().getMethod("getCountAvailable").invoke(entry));
    }

    @ParameterizedTest @CsvSource({"1,0,60", "1,40,20", "1,60,0", "2,40,88", "2,127,1", "2,128,0", "2,200,0", "2,2147483647,0"})
    void displayedGapAndColorUseTheActualMultiplierExactlyOnce(int multiplier, int available, int gap) throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of(), List.of());
        var entry = new MaterialListEntry(new ItemStack(Items.STONE), 64, 60, 0, available);
        var list = (MaterialListBase) list(getClass().getClassLoader(), entry, multiplier);
        var hud = new MixinMaterialListHudRenderer() {};
        var widget = new MixinWidgetMaterialListEntry() {};
        field(hud, "materialList", list);
        field(widget, "materialList", list);
        field(widget, "entry", entry);
        int hudMultiplier = (int) invoke(hud, "useCollectionGap", MaterialListBase.class, list);
        int hudDemand = (int) invoke(hud, "collectionDemand", MaterialListEntry.class, entry);
        assertEquals(gap, (hudDemand - entry.getCountAvailable()) * hudMultiplier);
        assertEquals(1, invoke(widget, "useCollectionGap", MaterialListBase.class, list));
        assertEquals(64 * multiplier, invoke(widget, "scaledTotal", MaterialListEntry.class, entry));
        assertEquals(gap, invoke(widget, "collectionGap", MaterialListEntry.class, entry));
        assertEquals(gap == 0 ? GuiBase.TXT_GREEN : GuiBase.TXT_RED, invoke(widget, "greenByNetGap", null, null));
        // Inactive cache must preserve the dependency's own calculation and color behavior.
        VoidWarehouseMaterialCache.get().clear();
        assertEquals(multiplier, invoke(hud, "useCollectionGap", MaterialListBase.class, list));
        assertEquals(60, invoke(hud, "collectionDemand", MaterialListEntry.class, entry));
        assertEquals(multiplier, invoke(widget, "useCollectionGap", MaterialListBase.class, list));
        assertEquals(64, invoke(widget, "scaledTotal", MaterialListEntry.class, entry));
        assertEquals(60, invoke(widget, "collectionGap", MaterialListEntry.class, entry));
        assertEquals(GuiBase.TXT_GREEN, invoke(widget, "greenByNetGap", null, null));
        assertEquals(GuiBase.TXT_GOLD, invoke(widget, "goldByNetGap", null, null));
    }

    private static Object invoke(Object instance, String name, Class<?> argumentType, Object argument) throws Exception {
        var type = instance.getClass().getSuperclass();
        var method = argumentType == null ? type.getDeclaredMethod("litematicaPrinter$" + name)
                : type.getDeclaredMethod("litematicaPrinter$" + name, argumentType);
        method.setAccessible(true);
        return argumentType == null ? method.invoke(instance) : method.invoke(instance, argument);
    }

    @ParameterizedTest @CsvSource({"1,39,20,true", "1,40,20,false", "1,20,20,true", "2,64,64,false"})
    void filteringAndHideAvailableRespectWorldDemandAndCombinedStock(int multiplier, int inventory, long warehouse,
                                                                      boolean missing) throws Exception {
        VoidWarehouseMaterialCache.get().apply(List.of("minecraft:stone"), List.of(warehouse));
        var loader = new MaterialListTestLoader();
        var list = list(loader, entry(loader, 64, 60, 20, inventory), multiplier);
        field(list, "hideAvailable", true);
        list.getClass().getMethod("recreateFilteredList").invoke(list);
        assertEquals(missing ? 1 : 0, ((List<?>) field(list, "materialListFiltered", null)).size());
        assertEquals(missing ? 1 : 0, ((List<?>) field(list, "materialListPreFiltered", null)).size());
    }
}
//#endif
