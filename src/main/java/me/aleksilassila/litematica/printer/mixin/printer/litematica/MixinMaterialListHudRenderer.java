package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCounts;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use the native demand-minus-availability path for every multiplier while the cache is active. */
@Mixin(value = MaterialListHudRenderer.class, remap = false)
public abstract class MixinMaterialListHudRenderer {
    @Shadow @Final protected MaterialListBase materialList;

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/materials/MaterialListBase;getMultiplier()I"), remap = false)
    private int litematicaPrinter$useCollectionGap(MaterialListBase list) {
        return VoidWarehouseMaterialCache.get().isReady() ? 1 : list.getMultiplier();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/materials/MaterialListEntry;getCountMissing()I"), remap = false)
    private int litematicaPrinter$collectionDemand(MaterialListEntry entry) {
        // Stock may refresh before the cached HUD list; the native subtraction must stay nonnegative.
        return VoidWarehouseMaterialCache.get().isReady()
                ? Math.max(entry.getCountAvailable(), VoidWarehouseMaterialCounts.demand(
                        entry.getCountTotal(), entry.getCountMissing(), this.materialList.getMultiplier()))
                : entry.getCountMissing();
    }
}
//#endif
