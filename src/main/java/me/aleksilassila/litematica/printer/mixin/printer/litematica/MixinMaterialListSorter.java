package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCounts;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sort missing materials by the displayed collection gap without changing world progress. */
@Mixin(value = MaterialListSorter.class, remap = false)
public abstract class MixinMaterialListSorter {
    @Shadow @Final private MaterialListBase materialList;

    @Redirect(method = "compare", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/materials/MaterialListEntry;getCountMissing()I"), remap = false)
    private int litematicaPrinter$collectionGap(MaterialListEntry entry) {
        return VoidWarehouseMaterialCache.get().isReady()
                ? VoidWarehouseMaterialCounts.netGap(VoidWarehouseMaterialCounts.demand(
                        entry.getCountTotal(), entry.getCountMissing(), this.materialList.getMultiplier()), entry.getCountAvailable())
                : entry.getCountMissing();
    }
}
//#endif
