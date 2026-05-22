package me.aleksilassila.litematica.printer.mixin.printer.litematica;


import fi.dy.masa.litematica.util.InventoryUtils;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(InventoryUtils.class)
public class MixinInventoryUtils {
    /**
     * @author BlinkWhite
     * @reason 去除优先选择目前已选择的槽位
     */
    @Overwrite
    private static int getPickBlockTargetSlot(Player player) {
        if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
            return -1;
        }
        int slotNum;
        if (InventoryUtilsAccessor.getNextPickSlotIndex() >= InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().size()) {
            InventoryUtilsAccessor.setNextPickSlotIndex(0);
        }
        for (int i = 0; i < InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().size(); ++i) {
            slotNum = InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().get(InventoryUtilsAccessor.getNextPickSlotIndex());

            InventoryUtilsAccessor.setNextPickSlotIndex(InventoryUtilsAccessor.getNextPickSlotIndex() + 1);

            if (InventoryUtilsAccessor.getNextPickSlotIndex() >= InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().size()) {
                InventoryUtilsAccessor.setNextPickSlotIndex(0);
            }
            if (InventoryUtilsAccessor.canPickToSlot(player.getInventory(), slotNum)) {
                return slotNum;
            }
        }
        return -1;
    }
}
