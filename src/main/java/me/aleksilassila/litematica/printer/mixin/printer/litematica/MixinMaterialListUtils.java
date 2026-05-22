package me.aleksilassila.litematica.printer.mixin.printer.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC >= 260000
//$$ import fi.dy.masa.malilib.util.data.ItemType;
//#else
import fi.dy.masa.malilib.util.ItemType;
//#endif

@Mixin(MaterialListUtils.class)
public class MixinMaterialListUtils {
    //#if MC == 12111
    @SuppressWarnings("deprecation")
    //#endif
    @WrapOperation(method = "getMaterialList", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/materials/MaterialListUtils;getInventoryItemCounts(Lnet/minecraft/world/Container;)Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"))
    private static Object2IntOpenHashMap<ItemType> initApplySelectionArea(Container inv, Operation<Object2IntOpenHashMap<ItemType>> original) {
        return original.call(inv);
    }
    //#if MC == 12111
    @SuppressWarnings("deprecation")
    //#endif
    @WrapOperation(method = "updateAvailableCounts", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/materials/MaterialListUtils;getInventoryItemCounts(Lnet/minecraft/world/Container;)Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"))
    private static Object2IntOpenHashMap<ItemType> updateApplySelectionArea(Container inv, Operation<Object2IntOpenHashMap<ItemType>> original) {
        return original.call(inv);
    }
}
