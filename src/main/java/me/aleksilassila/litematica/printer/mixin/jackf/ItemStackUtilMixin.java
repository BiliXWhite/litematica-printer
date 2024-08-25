package me.aleksilassila.litematica.printer.mixin.jackf;


//#if MC >= 12001
//$$
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import fi.dy.masa.malilib.util.StringUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.Utils.PinYinSearch;
//$$ import net.minecraft.enchantment.Enchantment;
//$$ import net.minecraft.enchantment.EnchantmentHelper;
//$$ import net.minecraft.item.ItemStack;
//$$ import net.minecraft.registry.RegistryKey;
//$$ import net.minecraft.registry.entry.RegistryEntry;
//$$ import net.minecraft.util.Language;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import red.jackf.chesttracker.impl.util.ItemStacks;
//$$ import net.minecraft.registry.Registries;
//$$
//#if MC > 12004
//$$ import net.minecraft.component.type.ItemEnchantmentsComponent;
//#endif
//$$
//$$ @Mixin(ItemStacks.class)
//$$ public class ItemStackUtilMixin {
//$$     @Inject(at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"), method = "enchantmentPredicate", cancellable = true)
//$$     private static void stackEnchantmentFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
        //#if MC > 12004
        //$$ ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        //$$ if (enchantments.getEnchantments().stream()
        //$$         .anyMatch(ench -> {
                    //#if MC > 12006
                    //$$ RegistryEntry<Enchantment> ench1 = ench;
                    //$$ RegistryKey<Enchantment> enchantmentRegistryKey = ench1.getKey().get();
                    //$$ String translationKey = enchantmentRegistryKey.getValue().toTranslationKey();
                    //$$ if (testLang(translationKey, filter)) return true;
                    //$$ String translate = StringUtils.translate(translationKey);
                    //$$ return translate != null && (translate.contains(filter) || PinYinSearch.hasPinYin(translate, filter));
                    //#else
                    //$$ if (testLang(ench.value().getTranslationKey(), filter)) return true;
                    //$$ var resloc = Registries.ENCHANTMENT.getId(ench.value());
                    //$$ return resloc != null && (resloc.toString().contains(filter) || PinYinSearch.hasPinYin(resloc.toString(), filter));
                    //#endif
        //$$         })) cir.setReturnValue(true);
        //#else
        //$$ var enchantments = EnchantmentHelper.get(stack);
        //$$ if (enchantments.isEmpty()) return;
        //$$ if (enchantments.keySet().stream()
        //$$         .anyMatch(ench -> {
        //$$             if (testLang(ench.getTranslationKey(), filter)) return true;
        //$$             var resloc = Registries.ENCHANTMENT.getKey(ench);
        //$$             return resloc.isPresent() && PinYinSearch.hasPinYin(resloc.toString(), filter);
        //$$         })
        //$$ ) cir.setReturnValue(true);
        //#endif
//$$     }
//$$
//$$     @Shadow(remap = false)
//$$     private static boolean testLang(String key, String filter) {
//$$         return false;
//$$     }
//$$
//$$     //    @Inject(at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), method = "potionOrEffectPredicate", cancellable = true)
//$$ //    private static void stackPotionFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
//$$ //
//$$ //        Potion potion = PotionContentsComponent.getPotion(stack);
//$$ //        if (potion != Potions.EMPTY) {
//$$ //            var langKey = potion.finishTranslationKey(stack.getTranslationKey() + ".effect.");
//$$ //            if (testLang(langKey, filter)) return;
//$$ //            var resloc = Registries.POTION.getKey(potion);
//$$ //            //noinspection ConstantValue
//$$ //            if (resloc.isPresent() && PinYinSearch.hasPinYin(resloc.toString(), filter)) cir.setReturnValue(true);
//$$ //        }
//$$ //        // specific effects
//$$ //        var effects = PotionContentsComponent.getPotionEffects(stack);
//$$ //        for (StatusEffectInstance effect : effects) {
//$$ //            var langKey = effect.getTranslationKey();
//$$ //            if (testLang(langKey, filter)) return;
//$$ //            var resloc = Registries.STATUS_EFFECT.getKey(effect.getEffectType());
//$$ //            if (resloc.isPresent() && PinYinSearch.hasPinYin(resloc.toString(), filter)) cir.setReturnValue(true);
//$$ //        }
//$$ //    }
//$$     @WrapOperation(at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"),method = "potionOrEffectPredicate")
//$$     private static boolean potionOrEffectPredicate(String instance, CharSequence s, Operation<Boolean> original){
//$$         return instance.contains(s) || PinYinSearch.hasPinYin(instance,s.toString());
//$$     }
//$$
//$$     @Inject(at = @At("HEAD"), method = "tagPredicate", cancellable = true)
//$$     private static void stackTagFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
//$$         if (stack.getRegistryEntry().streamTags().anyMatch(tag ->
//$$                 PinYinSearch.hasPinYin(tag.id().getPath(), filter)))
//$$             cir.setReturnValue(true);
//$$     }
//$$
//$$     @WrapOperation(at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"),method = "tooltipPredicate")
//$$     private static boolean tooltipPredicate(String instance, CharSequence s, Operation<Boolean> original){
//$$         return instance.contains(s) || PinYinSearch.hasPinYin(instance,s.toString());
//$$     }
//$$ //    @Inject(at = @At("HEAD"), method = "tooltipPredicate", cancellable = true)
//$$ //    private static void stackTooltipFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
//$$ //        var player = MinecraftClient.getInstance().player;
//$$ //        if (player == null) cir.setReturnValue(false);
//$$ //        var advanced = MinecraftClient.getInstance().options.advancedItemTooltips ? ADVANCED : TooltipType.BASIC;
//$$ //        for (Text line : stack.getTooltip(player, advanced)) {
//$$ ////            if (line.getString().toLowerCase().contains(filter)) cir.setReturnValue(true);
//$$ //            if (PinYinSearch.hasPinYin(line.getString(), filter)) cir.setReturnValue(true);
//$$ //        }
//$$ //    }
//$$
//$$     @Inject(at = @At("HEAD"), method = "testLang", cancellable = true, remap = false)
//$$     private static void testLang(String key, String filter, CallbackInfoReturnable<Boolean> cir) {
//$$         if (Language.getInstance().hasTranslation(key) &&
//$$                 PinYinSearch.hasPinYin(Language.getInstance().get(key).toLowerCase(), filter))
//$$             cir.setReturnValue(true);
//$$     }
//$$
//$$     @Inject(at = @At("HEAD"), method = "namePredicate", cancellable = true)
//$$     private static void stackNameFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
//$$         boolean b = PinYinSearch.hasPinYin(stack.getName().getString(), filter);
//$$         if (b) cir.setReturnValue(true);
//$$     }
//$$ //    @Inject(at = @At("HEAD"),method = "anyTextFilter", cancellable = true)
//$$ //    private static void anyTextFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir){
//$$ ////        System.out.println(filter);
//$$ ////        cir.setReturnValue(true);
//$$ //    }
//$$ }
//#endif