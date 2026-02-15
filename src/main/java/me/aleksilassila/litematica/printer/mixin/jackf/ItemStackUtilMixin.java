package me.aleksilassila.litematica.printer.mixin.jackf;


//#if MC >= 12001

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.util.StringUtils;
import me.aleksilassila.litematica.printer.utils.PinYinSearchUtils;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.impl.util.ItemStacks;

//#if MC > 12004
import net.minecraft.world.item.enchantment.ItemEnchantments;
//#endif

//#if MC <= 12006
//$$ import net.minecraft.core.registries.BuiltInRegistries;
//#endif

@Mixin(value = ItemStacks.class, remap = false)
public class ItemStackUtilMixin {
    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"), method = "enchantmentPredicate", cancellable = true)
    private static void stackEnchantmentFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
        //#if MC > 12004
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.keySet().stream()
                .anyMatch(ench -> {
                    //#if MC > 12006
                    ResourceKey<Enchantment> enchantmentRegistryKey = ench.unwrapKey().get();
                    String translationKey = enchantmentRegistryKey.identifier().toLanguageKey();
                    if (testLang(translationKey, filter)) return true;
                    String translate = StringUtils.translate(translationKey);
                    return translate != null && (translate.contains(filter) || PinYinSearchUtils.hasPinYin(translate, filter));
                    //#else
                    //$$ if (testLang(ench.value().getDescriptionId(), filter)) return true;
                    //$$ var resloc = BuiltInRegistries.ENCHANTMENT.getKey(ench.value());
                    //$$ return resloc != null && (resloc.toString().contains(filter) || PinYinSearchUtils.hasPinYin(resloc.toString(), filter));
                    //#endif
                })) cir.setReturnValue(true);
        //#else
        //$$ var enchantments = EnchantmentHelper.getEnchantments(stack);
        //$$ if (enchantments.isEmpty()) return;
        //$$ if (enchantments.keySet().stream()
        //$$         .anyMatch(ench -> {
        //$$             if (testLang(ench.getDescriptionId(), filter)) return true;
        //$$              var resloc = BuiltInRegistries.ENCHANTMENT.getKey(ench);
        //$$             return resloc != null && PinYinSearchUtils.hasPinYin(resloc.getNamespace(), filter);
        //$$         })
        //$$ ) cir.setReturnValue(true);
        //#endif
    }

    @Shadow(remap = false)
    private static boolean testLang(String key, String filter) {
        return false;
    }

    @WrapOperation(at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"),method = "potionOrEffectPredicate")
    private static boolean potionOrEffectPredicate(String instance, CharSequence s, Operation<Boolean> original){
        return instance.contains(s) || PinYinSearchUtils.hasPinYin(instance,s.toString());
    }

    @Inject(at = @At("HEAD"), method = "tagPredicate", cancellable = true)
    private static void stackTagFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItemHolder().tags().anyMatch(tag ->
                PinYinSearchUtils.hasPinYin(tag.location().getPath(), filter)))
            cir.setReturnValue(true);
    }

    @WrapOperation(at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"),method = "tooltipPredicate")
    private static boolean tooltipPredicate(String instance, CharSequence s, Operation<Boolean> original){
        return instance.contains(s) || PinYinSearchUtils.hasPinYin(instance,s.toString());
    }

    @Inject(at = @At("HEAD"), method = "testLang", cancellable = true, remap = false)
    private static void testLang(String key, String filter, CallbackInfoReturnable<Boolean> cir) {
        if (Language.getInstance().has(key) &&
                PinYinSearchUtils.hasPinYin(Language.getInstance().getOrDefault(key).toLowerCase(), filter))
            cir.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "namePredicate", cancellable = true)
    private static void stackNameFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir) {
        boolean b = PinYinSearchUtils.hasPinYin(stack.getHoverName().getString(), filter);
        if (b) cir.setReturnValue(true);
    }
//    @Inject(at = @At("HEAD"),method = "anyTextFilter", cancellable = true)
//    private static void anyTextFilter(ItemStack stack, String filter, CallbackInfoReturnable<Boolean> cir){
////        System.out.println(filter);
////        cir.setReturnValue(true);
//    }
}
//#else
//$$ import me.aleksilassila.litematica.printer.mixin_extension.Pointless;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ @Mixin(value = Pointless.class)
//$$ public class ItemStackUtilMixin { }
//#endif