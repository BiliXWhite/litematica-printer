package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCounts;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Display collection gaps locally, leaving the shared entry's world-progress semantics intact. */
@Mixin(value = WidgetMaterialListEntry.class, remap = false)
public abstract class MixinWidgetMaterialListEntry {
    @Shadow @Final private MaterialListEntry entry;
    @Shadow @Final private MaterialListBase materialList;

    @Redirect(method = {"render", "postRenderHovered"}, at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/materials/MaterialListBase;getMultiplier()I"), remap = false)
    private int litematicaPrinter$useCollectionGap(MaterialListBase list) {
        return VoidWarehouseMaterialCache.get().isReady() ? 1 : list.getMultiplier();
    }

    @Redirect(method = {"render", "postRenderHovered"}, at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/materials/MaterialListEntry;getCountTotal()I"), remap = false)
    private int litematicaPrinter$scaledTotal(MaterialListEntry entry) {
        return VoidWarehouseMaterialCache.get().isReady()
                ? VoidWarehouseMaterialCounts.demand(entry.getCountTotal(), entry.getCountTotal(), this.materialList.getMultiplier())
                : entry.getCountTotal();
    }

    @Redirect(method = {"render", "postRenderHovered"}, at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/materials/MaterialListEntry;getCountMissing()I"), remap = false)
    private int litematicaPrinter$collectionGap(MaterialListEntry entry) {
        return VoidWarehouseMaterialCache.get().isReady()
                ? VoidWarehouseMaterialCounts.netGap(VoidWarehouseMaterialCounts.demand(
                        entry.getCountTotal(), entry.getCountMissing(), this.materialList.getMultiplier()), entry.getCountAvailable())
                : entry.getCountMissing();
    }

    @Redirect(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lfi/dy/masa/malilib/gui/GuiBase;TXT_GREEN:Ljava/lang/String;"), remap = false)
    private String litematicaPrinter$greenByNetGap() {
        if (!VoidWarehouseMaterialCache.get().isReady()) return GuiBase.TXT_GREEN;
        return this.entry != null && this.litematicaPrinter$collectionGap(this.entry) == 0 ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lfi/dy/masa/malilib/gui/GuiBase;TXT_GOLD:Ljava/lang/String;"), remap = false)
    private String litematicaPrinter$goldByNetGap() {
        return VoidWarehouseMaterialCache.get().isReady() ? GuiBase.TXT_RED : GuiBase.TXT_GOLD;
    }
}
//#endif
