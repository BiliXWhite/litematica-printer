package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import fi.dy.masa.malilib.util.InventoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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

//#if MC > 12004
//$$
//#else

//#endif



@Mixin(value = ChestTrackerScreen.class)
public abstract class ChestTrackerScreenMixin extends Screen {
    @Shadow(remap = false) private ItemListWidget itemList;
    @Shadow(remap = false) private VerticalScrollWidget scroll;
    @Shadow(remap = false) private List<ItemStack> items = Collections.emptyList();

    protected ChestTrackerScreenMixin(Text title) {
        super(title);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    private void filter(String filter){
        new Thread(() -> {
            //        SearchablesUtil a;
            //濳影盒等搜索
            List<ItemStack> filtered = new ArrayList<>(items.stream().filter(stack -> {
                return InventoryUtils.getStoredItems(stack, -1).stream().anyMatch((stack2) -> {

                    //#if MC > 12004
                    //$$
                    //#else

                    //#endif
                    return ItemStacks.defaultPredicate(stack2,filter);

//                return stack2.getName().getString().toLowerCase().contains(filter) ||
//                        //#if MC > 12004
//                        //$$
//                        //#else
//                        stack2.hasCustomName() &&
//                        //#endif
//                        stack2.getItem().getName(stack2).getString().toLowerCase().contains(filter) ||
//
//                        //#if MC > 12004
//                        //$$
//                        //#else
//                        stack2.getNbt() != null && stack2.getNbt().toString().toLowerCase().contains(filter) ||
//                        //#endif
//
//                        PinYinSearch.hasPinYin(stack2.getName().getString().toLowerCase(), filter) ||
//                        //#if MC > 12004
//                        //$$ (Optional.ofNullable(stack2.getComponentChanges().get(DataComponentTypes.CUSTOM_NAME)).isPresent() &&
//                        //#else
//                         (stack2.hasCustomName() &&
//                        //#endif
//                        PinYinSearch.hasPinYin(stack2.getItem().getName(stack2).getString().toLowerCase(), filter)) ||
//                        Registries.ITEM.getId(stack2.getItem()).toString().contains(filter)
//                        //#if MC > 12004
//                        //$$
//                        //#else
//                        ||  stack2.getNbt() != null && PinYinSearch.hasPinYin(stack2.getNbt().toString().toLowerCase(), filter)
//                        //#endif
//                ;
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
            filtered = filtered.stream().distinct().toList();
            this.itemList.setItems(filtered);
            ChestTrackerConfig.Gui guiConfig = ((ChestTrackerConfig)ChestTrackerConfig.INSTANCE.instance()).gui;
            this.scroll.setDisabled(filtered.size() <= guiConfig.gridWidth * guiConfig.gridHeight);
        }).start();
    }
    @Shadow(remap = false) private Identifier currentMemoryKey;

    @Inject(at = @At("HEAD"), method = "updateItems",remap = false)
    private void upDateItems(CallbackInfo ci) {
        MemoryUtils.currentMemoryKey = currentMemoryKey;
    }

    @Shadow public abstract void close();

    @Inject(at = @At("HEAD"),method = "keyPressed", cancellable = true)
    public void keyPressed1(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir){
        if (MinecraftClient.getInstance().options.inventoryKey.matchesKey(keyCode, scanCode) && !(this.getFocused() instanceof TextFieldWidget) ) {
            this.close();
        cir.setReturnValue(true);
        }
    }
}
//#endif