package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

@Mixin(value = ChestTrackerScreen.class)
public abstract class ChestTrackerScreenMixin extends Screen {
    @Shadow(remap = false) private ItemListWidget itemList;
    @Shadow(remap = false) private VerticalScrollWidget scroll;
    @Shadow(remap = false) private List<ItemStack> items = Collections.emptyList();

    protected ChestTrackerScreenMixin(Component title) {
        super(title);
    }

    /**
     * @author zhaixianyu
     * @reason 支持搜索箱子内物品
     */
    @Overwrite(remap = false)
    private void filter(String filter){
        new Thread(() -> {
            //濳影盒等搜索
            List<ItemStack> filtered = new ArrayList<>(items.stream().filter(stack -> InventoryUtils.getStoredItems(stack, -1).stream().anyMatch((stack2) -> {

                //#if MC > 12004

                //#else
                //$$
                //#endif
                return ItemStacks.defaultPredicate(stack2,filter);
            })).toList());
            filtered.addAll(SearchablesUtil.ITEM_STACK.filterEntries(this.items, filter.toLowerCase()));
            filtered = filtered.stream().distinct().toList();
            this.itemList.setItems(filtered);
            ChestTrackerConfig.Gui guiConfig = ChestTrackerConfig.INSTANCE.instance().gui;
            this.scroll.setDisabled(filtered.size() <= guiConfig.gridWidth * guiConfig.gridHeight);
        }).start();
    }
    @Shadow(remap = false) private ResourceLocation currentMemoryKey;

    @Inject(at = @At("HEAD"), method = "updateItems",remap = false)
    private void upDateItems(CallbackInfo ci) {
        MemoryUtils.currentMemoryKey = currentMemoryKey;
    }

    @Shadow public abstract void onClose();

    @Inject(at = @At("HEAD"),method = "keyPressed", cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir){
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode) && !(this.getFocused() instanceof MultilineTextField) ) {
            this.onClose();
            cir.setReturnValue(true);
        }
     }
}
//#endif