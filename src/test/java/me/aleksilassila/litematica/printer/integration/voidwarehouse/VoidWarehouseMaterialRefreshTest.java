//#if MC >= 12111
package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import me.aleksilassila.litematica.printer.mixin.printer.malilib.GuiListBaseAccessor;
import me.aleksilassila.litematica.printer.config.Configs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import neko.voidwarehouse.restock.PrinterQueryResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialConsumersTest.*;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the real packet handler, material filtering and browser sorting without drawing a window. */
class VoidWarehouseMaterialRefreshTest {
    @BeforeAll static void initialize() { bootstrap(); }
    @AfterEach void clear() { VoidWarehouseMaterialCache.get().clear(); }

    public static class HeadlessGui extends GuiMaterialList implements GuiListBaseAccessor {
        public HeadlessGui() { super(null); }
        @Override public WidgetListBase<?, ?> litematicaPrinter$getListWidget() { return getListWidget(); }
        @Override public void initGui() { throw new AssertionError("Do not recreate input fields on inventory refresh"); }
    }

    public static class HeadlessBrowser extends WidgetListMaterialList {
        public HeadlessBrowser() { super(0, 0, 100, 100, null); }
        public int rebuilds;
        public String filter;
        @Override protected void reCreateListEntryWidgets() { rebuilds++; }
        @Override protected boolean hasFilter() { return filter != null; }
        @Override protected String getFilterText() { return filter; }
    }

    static Object allocate(Class<?> type) throws Exception {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return ((sun.misc.Unsafe) field.get(null)).allocateInstance(type);
    }

    static void set(Object instance, Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value);
    }

    static final class Fixture implements AutoCloseable {
        final Minecraft oldClient = Minecraft.getInstance();
        final Minecraft client = (Minecraft) allocate(Minecraft.class);
        final MaterialListTestLoader loader = new MaterialListTestLoader(
                VoidWarehouseMaterialNetwork.class.getName(), "fi.dy.masa.litematica.data.DataManager",
                GuiMaterialList.class.getName(), WidgetListMaterialList.class.getName(),
                HeadlessGui.class.getName(), HeadlessBrowser.class.getName());
        final Class<?> network;
        final Object stone;
        final Object dirt;
        final Object list;
        final Object gui;
        final Object browser;
        final Object screenOwner;

        Fixture(boolean hide, int multiplier) throws Exception {
            set(null, Minecraft.class, "instance", client);
            //#if MC > 260100
            //$$ var guiField = Minecraft.class.getDeclaredField("gui");
            //$$ screenOwner = allocate(guiField.getType());
            //$$ set(client, Minecraft.class, "gui", screenOwner);
            //#else
            screenOwner = client;
            //#endif
            VoidWarehouseMaterialCache.get().clear();
            network = loader.loadClass(VoidWarehouseMaterialNetwork.class.getName());
            stone = entry(loader, 100, 100, 0, 0);
            dirt = stone.getClass().getConstructor(ItemStack.class, int.class, int.class, int.class, int.class)
                    .newInstance(new ItemStack(Items.DIRT), 80, 80, 0, 0);
            list = list(loader, stone, multiplier);
            field(list, "materialListAll", ImmutableList.of(stone, dirt));
            field(list, "materialListPreFiltered", new ArrayList<>(List.of(stone, dirt)));
            field(list, "ignored", new HashSet<>());
            field(list, "hideAvailable", hide);
            field(list, "sortCriteria", loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase$SortCriteria")
                    .getField("COUNT_MISSING").get(null));
            gui = allocate(loader.loadClass(HeadlessGui.class.getName()));
            field(gui, "materialList", list);
            browser = allocate(loader.loadClass(HeadlessBrowser.class.getName()));
            field(browser, "gui", gui);
            field(browser, "sorter", loader.loadClass("fi.dy.masa.litematica.materials.MaterialListSorter")
                    .getConstructor(loader.loadClass("fi.dy.masa.litematica.materials.MaterialListBase")).newInstance(list));
            field(browser, "listContents", new ArrayList<>());
            field(browser, "shouldSortList", true);
            field(browser, "scrollbarRestored", true);
            field(gui, "widget", browser);
            set(screenOwner, screenOwner.getClass(), "screen", gui);
            var data = loader.loadClass("fi.dy.masa.litematica.data.DataManager");
            field(data.getMethod("getInstance").invoke(null), "materialList", list);
            browser.getClass().getMethod("refreshEntries").invoke(browser);
            set(null, network, "latestNonce", 7);
        }

        void receive(int nonce, int status, long stock) throws Exception {
            var method = network.getDeclaredMethod("receive", PrinterQueryResultPayload.class, ClientPlayNetworking.Context.class);
            method.setAccessible(true);
            method.invoke(null, new PrinterQueryResultPayload(nonce, status, List.of("minecraft:stone"), List.of(stock)), null);
        }

        List<?> visible() throws Exception { return (List<?>) browser.getClass().getMethod("getCurrentEntries").invoke(browser); }
        int rebuilds() throws Exception { return (int) field(browser, "rebuilds", null); }

        @Override public void close() throws Exception { set(null, Minecraft.class, "instance", oldClient); }
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void lateSnapshotResortsTheAlreadyOpenBrowser(int multiplier) throws Exception {
        try (var f = new Fixture(false, multiplier)) {
            assertEquals(List.of(f.stone, f.dirt), f.visible());
            f.receive(7, PrinterQueryResultPayload.OK, 100L * multiplier);
            assertEquals(List.of(f.dirt, f.stone), f.visible());
        }
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void hiddenEntryReturnsWhenLaterStockIsInsufficient(int multiplier) throws Exception {
        try (var f = new Fixture(true, multiplier)) {
            f.receive(7, PrinterQueryResultPayload.OK, 100L * multiplier);
            assertEquals(List.of(f.dirt), f.visible());
            f.receive(7, PrinterQueryResultPayload.OK, 0);
            assertEquals(List.of(f.stone, f.dirt), f.visible());
        }
    }

    @Test void staleNonceDoesNotRefreshOrChangeStock() throws Exception {
        try (var f = new Fixture(true, 1)) {
            f.receive(6, PrinterQueryResultPayload.OK, 100);
            assertFalse(VoidWarehouseMaterialCache.get().isReady());
            assertEquals(1, f.rebuilds());
            assertEquals(List.of(f.stone, f.dirt), f.visible());
        }
    }

    @ParameterizedTest @ValueSource(ints = {PrinterQueryResultPayload.RULE_OFF, PrinterQueryResultPayload.BAD_REQUEST,
            PrinterQueryResultPayload.THROTTLED})
    void rejectionRestoresEntriesHiddenByWarehouseStock(int status) throws Exception {
        try (var f = new Fixture(true, 1)) {
            f.receive(7, PrinterQueryResultPayload.OK, 100);
            f.receive(7, status, 0);
            assertFalse(VoidWarehouseMaterialCache.get().isReady());
            assertEquals(List.of(f.stone, f.dirt), f.visible());
            assertEquals(3, f.rebuilds());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void disableOrDisconnectRestoresEntriesOnceAndIgnoresTheInFlightReply(boolean disable) throws Exception {
        boolean oldEnabled = Configs.Special.VOID_WAREHOUSE_MATERIAL_LIST.getBooleanValue();
        try (var f = new Fixture(true, 1)) {
            f.receive(7, PrinterQueryResultPayload.OK, 100);
            if (disable) {
                f.client.player = (LocalPlayer) allocate(LocalPlayer.class);
                f.client.level = (ClientLevel) allocate(ClientLevel.class);
                Configs.Special.VOID_WAREHOUSE_MATERIAL_LIST.setBooleanValue(false);
            }
            f.network.getMethod("tick").invoke(null);
            f.network.getMethod("tick").invoke(null);
            f.receive(7, PrinterQueryResultPayload.OK, 100);
            assertFalse(VoidWarehouseMaterialCache.get().isReady());
            assertEquals(List.of(f.stone, f.dirt), f.visible());
            assertEquals(3, f.rebuilds(), "Repeated disabled/disconnected ticks must not keep rebuilding the GUI");
        } finally { Configs.Special.VOID_WAREHOUSE_MATERIAL_LIST.setBooleanValue(oldEnabled); }
    }

    @Test void refreshingPreservesIgnoredEntriesAndSearchText() throws Exception {
        try (var f = new Fixture(false, 1)) {
            f.list.getClass().getMethod("ignoreEntry", f.stone.getClass()).invoke(f.list, f.dirt);
            field(f.browser, "filter", "stone");
            f.receive(7, PrinterQueryResultPayload.OK, 100);
            assertEquals(List.of(f.stone), f.visible());
            assertEquals("stone", field(f.browser, "filter", null));
            set(f.browser, f.browser.getClass(), "filter", null);
            f.browser.getClass().getMethod("refreshEntries").invoke(f.browser);
            assertEquals(List.of(f.stone), f.visible(), "Explicitly ignored dirt must stay hidden without the search filter");
        }
    }

    @Test void closedScreenDoesNotRebuildButRestoresMaterialCandidates() throws Exception {
        try (var f = new Fixture(true, 1)) {
            f.receive(7, PrinterQueryResultPayload.OK, 100);
            assertEquals(List.of(f.dirt), f.visible());
            int beforeClose = f.rebuilds();
            set(f.screenOwner, f.screenOwner.getClass(), "screen", null);
            f.receive(7, PrinterQueryResultPayload.OK, 0);
            assertEquals(beforeClose, f.rebuilds());
            assertEquals(List.of(f.stone, f.dirt), f.list.getClass().getMethod("getMaterialsMissingOnly", boolean.class)
                    .invoke(f.list, true));
        }
    }
}
//#endif
