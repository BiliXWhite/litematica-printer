package me.aleksilassila.litematica.printer.mixin.jackf;

import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.PinYinSearch;
import me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

//        SearchablesUtil a;
        //濳影盒等搜索
        List<ItemStack> filtered = new ArrayList<>(items.stream().filter(stack -> {
            return InventoryUtils.getStoredItems(stack, -1).stream().anyMatch((stack2) -> {
                return stack2.getName().getString().toLowerCase().contains(filter) ||
                        stack2.hasCustomName() && stack2.getItem().getName(stack2).getString().toLowerCase().contains(filter) ||
                        stack2.getNbt() != null && stack2.getNbt().toString().toLowerCase().contains(filter) ||

                        PinYinSearch.getPinYin(stack2.getName().getString().toLowerCase()).stream().anyMatch(s -> s.contains(filter)) ||
                        stack2.hasCustomName() && PinYinSearch.getPinYin(stack2.getItem().getName(stack2).getString().toLowerCase()).stream().anyMatch(s -> s.contains(filter)) ||
                        Registries.ITEM.getId(stack.getItem()).toString().contains(filter) ||
                        stack2.getNbt() != null && PinYinSearch.getPinYin(stack2.getNbt().toString().toLowerCase()).stream().anyMatch(s -> s.contains(filter));
            });
        }).toList());

//        List<ItemStack> filteredItems = items.stream().filter((stack) -> {
//            return stack.getName().getString().toLowerCase().contains(filter) ||
//                    stack.hasCustomName() && stack.getItem().getName(stack).getString().toLowerCase().contains(filter) ||
//                    stack.getNbt() != null && stack.getNbt().toString().toLowerCase().contains(filter) ||
//                    (stack.isOf(Items.ENCHANTED_BOOK) && EnchantmentHelper.get(stack).entrySet().stream().anyMatch(e ->
//                            PinYinSearch.getPinYin(Text.translatable(e.getKey().getTranslationKey()).getString()).stream().anyMatch(s -> s.contains(filter)))) ||
//                    stack.hasCustomName() && PinYinSearch.getPinYin(stack.getItem().getName(stack).getString().toLowerCase()).stream().anyMatch(s -> s.contains(filter)) ||
//                    stack.getNbt() != null && PinYinSearch.getPinYin(stack.getNbt().toString().toLowerCase()).stream().anyMatch(s -> s.contains(filter)) ||
//                    Registries.ITEM.getId(stack.getItem()).toString().contains(filter) ||
//                    PinYinSearch.getPinYin(stack.getName().getString().toLowerCase()).stream().anyMatch(s -> s.contains(filter))
//                    ||
//                    InventoryUtils.getStoredItems(stack, -1).stream().anyMatch((stack2) ->{
//                        return stack2.getName().getString().toLowerCase().contains(filter) ||
//                                stack2.hasCustomName() && stack2.getItem().getName(stack2).getString().toLowerCase().contains(filter) ||
//                                stack2.getNbt() != null && stack2.getNbt().toString().toLowerCase().contains(filter) ||
//
//                                PinYinSearch.getPinYin(stack2.getName().getString().toLowerCase()).stream().anyMatch(s -> s.contains(filter)) ||
//                                stack2.hasCustomName() && PinYinSearch.getPinYin(stack2.getItem().getName(stack2).getString().toLowerCase()).stream().anyMatch(s -> s.contains(filter)) ||
//                                Registries.ITEM.getId(stack.getItem()).toString().contains(filter) ||
//                                stack2.getNbt() != null && PinYinSearch.getPinYin(stack2.getNbt().toString().toLowerCase()).stream().anyMatch(s -> s.contains(filter));
//                    });
//        }).collect(Collectors.toList());
//        filteredItems.addAll(filtered);
//        if(!nbtList.isEmpty()) filtered.addAll(nbtList);

        filtered.addAll(SearchablesUtil.ITEM_STACK.filterEntries(this.items, filter.toLowerCase()));
        this.itemList.setItems(filtered);
        ChestTrackerConfig.Gui guiConfig = ((ChestTrackerConfig)ChestTrackerConfig.INSTANCE.instance()).gui;
        this.scroll.setDisabled(filtered.size() <= guiConfig.gridWidth * guiConfig.gridHeight);
    }
}
