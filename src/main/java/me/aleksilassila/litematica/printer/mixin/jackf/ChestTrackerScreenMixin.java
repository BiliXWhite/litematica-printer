package me.aleksilassila.litematica.printer.mixin.jackf;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.config.ChestTrackerConfig;
import red.jackf.chesttracker.gui.screen.ChestTrackerScreen;
import red.jackf.chesttracker.gui.util.SearchablesUtil;
import red.jackf.chesttracker.gui.widget.ItemListWidget;
import red.jackf.chesttracker.gui.widget.VerticalScrollWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(ChestTrackerScreen.class)
public class ChestTrackerScreenMixin {
    @Shadow(remap = false) private ItemListWidget itemList;
    @Shadow(remap = false) private VerticalScrollWidget scroll;
    @Shadow(remap = false) private List<ItemStack> items = Collections.emptyList();
    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    private void filter(String filter){
        List<ItemStack> filtered = SearchablesUtil.ITEM_STACK.filterEntries(this.items, filter.toLowerCase());
        List<ItemStack> filtered2 = null;

        filtered.addAll(filtered2);
        this.itemList.setItems(filtered);
        ChestTrackerConfig.Gui guiConfig = ((ChestTrackerConfig)ChestTrackerConfig.INSTANCE.instance()).gui;
        this.scroll.setDisabled(filtered.size() <= guiConfig.gridWidth * guiConfig.gridHeight);
    }
}
