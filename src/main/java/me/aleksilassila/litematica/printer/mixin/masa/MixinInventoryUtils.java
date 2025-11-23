// TODO(Ravel): Failed to fully remap file: null
// TODO(Ravel): Failed to fully remap file: null
package me.aleksilassila.litematica.printer.mixin.masa;


import fi.dy.masa.litematica.util.InventoryUtils;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor.canPickToSlot;
import static me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor.setNextPickSlotIndex;

@Mixin(InventoryUtils.class)
public class MixinInventoryUtils {
    @Inject(at = @At("TAIL"),method = "schematicWorldPickBlock")
    private static void schematicWorldPickBlock(ItemStack stack, BlockPos pos, World schematicWorld, MinecraftClient mc, CallbackInfo ci) {
        if (mc.player != null && !ItemStack.areItemsAndComponentsEqual(mc.player.getMainHandStack(), stack
        ) && (
                LitematicaPrinterMod.CLOUD_INVENTORY.getBooleanValue() ||
                        LitematicaPrinterMod.QUICK_SHULKER.getBooleanValue()
        )) {
            me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.lastNeedItemList.add(stack.getItem());
            me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem();
        }
    }

//    /**
//     * @author BlinkWhite
//     * @reason 去除优先选择目前已选择的槽位
//     */
//    @Overwrite
//    private static int getPickBlockTargetSlot(PlayerEntity player) {
//        if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
//            return -1;
//        }
//
//        int slotNum;
//
//        if (InventoryUtilsAccessor.getNextPickSlotIndex() >= InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().size()) {
//            InventoryUtilsAccessor.setNextPickSlotIndex(0);
//        }
//
//        for (int i = 0; i < InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().size(); ++i) {
//            slotNum = InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().get(InventoryUtilsAccessor.getNextPickSlotIndex());
//
//            setNextPickSlotIndex(InventoryUtilsAccessor.getNextPickSlotIndex() + 1);
//
//            if (InventoryUtilsAccessor.getNextPickSlotIndex() >= InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().size()) {
//                InventoryUtilsAccessor.setNextPickSlotIndex(0);
//            }
//
//            if (canPickToSlot(player.getInventory(), slotNum)) {
//                return slotNum;
//            }
//        }
//
//        return -1;
//    }
}
