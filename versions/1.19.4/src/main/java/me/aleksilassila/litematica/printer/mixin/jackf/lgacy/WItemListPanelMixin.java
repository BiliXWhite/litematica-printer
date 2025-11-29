package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.PinYinSearch;
import net.minecraft.world.item.ItemStack;
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
        filteredItems = items.stream().filter((stack) -> stack.getHoverName().getString().toLowerCase().contains(filter) ||
                PinYinSearch.hasPinYin(stack.getHoverName().getString().toLowerCase(),filter) ||
                (stack.hasCustomHoverName() && stack.getItem().getName(stack).getString().toLowerCase().contains(filter) ||
                        PinYinSearch.hasPinYin(stack.getItem().getName(stack).getString().toLowerCase(),filter)) ||
                (stack.getTag() != null && (stack.getTag().toString().toLowerCase().contains(filter) ||
                        PinYinSearch.hasPinYin(stack.getTag().toString().toLowerCase(),filter))) ||

                fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1).stream().anyMatch((stack2) -> stack2.getHoverName().getString().toLowerCase().contains(filter) ||
                        PinYinSearch.hasPinYin(stack2.getHoverName().getString().toLowerCase(),filter) ||
                        (stack2.hasCustomHoverName() && stack2.getItem().getName(stack2).getString().toLowerCase().contains(filter) ||
                                PinYinSearch.hasPinYin(stack2.getItem().getName(stack2).getString().toLowerCase(),filter)) ||
                        (stack2.getTag() != null && (stack2.getTag().toString().toLowerCase().contains(filter) ||
                                PinYinSearch.hasPinYin(stack2.getTag().toString().toLowerCase(),filter))))).collect(Collectors.toList());
        pageCount = (filteredItems.size() - 1) / (columns * rows) + 1;
        currentPage = Math.min(currentPage, pageCount);
        if (pageChangeHook != null) {
            pageChangeHook.accept(currentPage, pageCount);
        }

    }
}
