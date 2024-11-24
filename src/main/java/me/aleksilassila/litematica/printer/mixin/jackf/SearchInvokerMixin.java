package me.aleksilassila.litematica.printer.mixin.jackf;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.client.api.events.SearchInvoker;


@Mixin(SearchInvoker.class)
public interface SearchInvokerMixin {
    @Inject(at = @At("RETURN"), method = "doSearch",remap = false)
    private static void doSearch(SearchRequest request, CallbackInfoReturnable<Boolean> cir) {
        if(LitematicaMixinMod.INVENTORY.getBooleanValue()){
            MemoryUtils.request = request;
            SearchItem.search(false);
            SearchItem.openInventory(0);
        }
    }
}
//#endif