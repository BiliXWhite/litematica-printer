package me.aleksilassila.litematica.printer.mixin.printer.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(MaterialListUtils.class)
public class MixinMaterialListUtils {
    @WrapOperation(method = "getMaterialList", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/materials/MaterialListUtils;getInventoryItemCounts(Lnet/minecraft/world/Container;)Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"))
    private static Object2IntOpenHashMap<ItemType> applySelectionArea(Container inv, Operation<Object2IntOpenHashMap<ItemType>> original) {
        Object2IntOpenHashMap<ItemType> result = original.call(inv);
        List<Object2IntOpenHashMap<ItemType>> selection = LitematicaUtils.getSelectionContainerItems();
        for (Object2IntOpenHashMap<ItemType> map : selection) {
            map.forEach(result::addTo);
        }
        return result;
    }

}
