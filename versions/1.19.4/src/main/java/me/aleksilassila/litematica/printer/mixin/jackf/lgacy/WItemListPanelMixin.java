package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.PinYinSearch;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import red.jackf.chesttracker.gui.widgets.WItemListPanel;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Mixin(WItemListPanel.class)
public class WItemListPanelMixin{

    @Shadow(remap = false) private List<ItemStack> filteredItems;

    @Shadow(remap = false) private List<ItemStack> items;

    @Shadow(remap = false) private String filter;

    @Shadow(remap = false) private int pageCount;

    @Shadow(remap = false) private int currentPage;

    @Shadow(remap = false) private @Nullable BiConsumer<Integer, Integer> pageChangeHook;

    @Shadow(remap = false) @Final private int columns;

    @Shadow(remap = false) @Final private int rows;
    /**
     * @author 2
     * @reason 2
     */
    @Overwrite(remap = false)
//    @Inject(at = @At("TAIL"),method = "updateFilter")
    private void updateFilter() {
        filteredItems = items.stream().filter((stack) -> stack.getName().getString().toLowerCase().contains(filter) ||
                PinYinSearch.hasPinYin(stack.getName().getString().toLowerCase(),filter) ||
                (stack.hasCustomName() && stack.getItem().getName(stack).getString().toLowerCase().contains(filter) ||
                        PinYinSearch.hasPinYin(stack.getItem().getName(stack).getString().toLowerCase(),filter)) ||
                (stack.getNbt() != null && (stack.getNbt().toString().toLowerCase().contains(filter) ||
                        PinYinSearch.hasPinYin(stack.getNbt().toString().toLowerCase(),filter))) ||

                fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1).stream().anyMatch((stack2) -> stack2.getName().getString().toLowerCase().contains(filter) ||
                        PinYinSearch.hasPinYin(stack2.getName().getString().toLowerCase(),filter) ||
                        (stack2.hasCustomName() && stack2.getItem().getName(stack2).getString().toLowerCase().contains(filter) ||
                                PinYinSearch.hasPinYin(stack2.getItem().getName(stack2).getString().toLowerCase(),filter)) ||
                        (stack2.getNbt() != null && (stack2.getNbt().toString().toLowerCase().contains(filter) ||
                                PinYinSearch.hasPinYin(stack2.getNbt().toString().toLowerCase(),filter))))).collect(Collectors.toList());
        pageCount = (filteredItems.size() - 1) / (columns * rows) + 1;
        currentPage = Math.min(currentPage, pageCount);
        if (pageChangeHook != null) {
            pageChangeHook.accept(currentPage, pageCount);
        }

    }
}
