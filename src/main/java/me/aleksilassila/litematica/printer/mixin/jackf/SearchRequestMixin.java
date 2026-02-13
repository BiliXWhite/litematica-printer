package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.whereisit.api.SearchRequest;

@Mixin(value = SearchRequest.class, remap = false)
public class SearchRequestMixin {
    @Inject(at = @At("RETURN"), method = "check", cancellable = true,remap = false)
    private static void check(ItemStack stack, SearchRequest request, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            boolean b = SearchItem.areStacksEquivalent(MemoryUtils.itemStack, stack);
            cir.setReturnValue(b);
        }
    }
}
//#else
//$$ import me.aleksilassila.litematica.printer.mixin_extension.Pointless;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ @Mixin(value = Pointless.class)
//$$ public class SearchRequestMixin { }
//#endif