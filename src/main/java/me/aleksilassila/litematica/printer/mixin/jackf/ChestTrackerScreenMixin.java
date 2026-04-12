package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.impl.compat.mods.searchables.SearchablesUtil;
import red.jackf.chesttracker.impl.config.ChestTrackerConfig;
import red.jackf.chesttracker.impl.gui.screen.ChestTrackerScreen;
import red.jackf.chesttracker.impl.gui.widget.ItemListWidget;
import red.jackf.chesttracker.impl.gui.widget.VerticalScrollWidget;
import red.jackf.chesttracker.impl.util.ItemStacks;

import java.util.*;

@Mixin(value = ChestTrackerScreen.class, remap = false)
public abstract class ChestTrackerScreenMixin extends Screen {
    @Shadow(remap = false)
    private ItemListWidget itemList;
    @Shadow(remap = false)
    private VerticalScrollWidget scroll;
    @Shadow(remap = false)
    private List<ItemStack> items = Collections.emptyList();
    @Shadow(remap = false)
    private Identifier currentMemoryKey;

    protected ChestTrackerScreenMixin(Component title) {
        super(title);
    }

    /**
     * @author zhaixianyu
     * @reason 支持搜索箱子内物品
     */
    @Overwrite(remap = false)
    private void filter(String filter) {
        ItemListWidget itemList = this.itemList;
        VerticalScrollWidget scroll = this.scroll;
        if (itemList == null || scroll == null) return;

        List<ItemStack> itemsSnapshot = new ArrayList<>(this.items);
        new Thread(() -> {
            List<ItemStack> filtered = new ArrayList<>(itemsSnapshot.stream()
                    .filter(stack -> InventoryUtils
                            .getStoredItems(stack, -1)
                            .stream()
                            .anyMatch((stack2) -> ItemStacks.defaultPredicate(stack2, filter))).toList()
            );
            filtered.addAll(SearchablesUtil.ITEM_STACK.filterEntries(itemsSnapshot, filter.toLowerCase()));
            filtered = filtered.stream().distinct().toList();

            if (Minecraft.getInstance().screen != this) return;
            itemList.setItems(filtered);
            ChestTrackerConfig.Gui guiConfig = ChestTrackerConfig.INSTANCE.instance().gui;
            scroll.setDisabled(filtered.size() <= guiConfig.gridWidth * guiConfig.gridHeight);
        }).start();
    }

    @Inject(at = @At("HEAD"), method = "updateItems", remap = false)
    private void upDateItems(CallbackInfo ci) {
        MemoryUtils.currentMemoryKey = currentMemoryKey;
    }
}
//#else
//$$ import me.aleksilassila.litematica.printer.mixin_extension.Pointless;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ @Mixin(value = Pointless.class)
//$$ public class ChestTrackerScreenMixin { }
//#endif