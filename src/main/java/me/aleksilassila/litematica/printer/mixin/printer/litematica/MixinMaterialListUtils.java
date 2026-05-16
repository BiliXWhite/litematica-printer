package me.aleksilassila.litematica.printer.mixin.printer.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
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
    @SuppressWarnings("deprecation")
    @WrapOperation(method = "getMaterialList", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/materials/MaterialListUtils;getInventoryItemCounts(Lnet/minecraft/world/Container;)Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"))
    private static Object2IntOpenHashMap<ItemType> initApplySelectionArea(Container inv, Operation<Object2IntOpenHashMap<ItemType>> original) {
        Object2IntOpenHashMap<ItemType> result = original.call(inv);
        LitematicaUtils.applySelectionArea(result);
        return result;
    }

    @SuppressWarnings("deprecation")
    @WrapOperation(method = "updateAvailableCounts", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/materials/MaterialListUtils;getInventoryItemCounts(Lnet/minecraft/world/Container;)Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"))
    private static Object2IntOpenHashMap<ItemType> updateApplySelectionArea(Container inv, Operation<Object2IntOpenHashMap<ItemType>> original) {
        Object2IntOpenHashMap<ItemType> result = original.call(inv);
        LitematicaUtils.applySelectionArea(result);
        return result;
    }
}
