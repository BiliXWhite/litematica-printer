package me.aleksilassila.litematica.printer.mixin.masa;


import fi.dy.masa.litematica.util.InventoryUtils;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.canPickToSlot;
import static me.aleksilassila.litematica.printer.printer.Printer.remoteItem;

@Mixin(InventoryUtils.class)
public class MixinInventoryUtils {
    @Inject(at = @At("TAIL"),method = "schematicWorldPickBlock")
    private static void schematicWorldPickBlock(ItemStack stack, BlockPos pos, World schematicWorld, MinecraftClient mc, CallbackInfo ci){
        if (mc.player != null && !ItemStack.areItemsAndComponentsEqual(mc.player.getMainHandStack(),stack) && (LitematicaMixinMod.INVENTORY.getBooleanValue() || LitematicaMixinMod.QUICK_SHULKER.getBooleanValue())) {
            remoteItem.add(stack.getItem());
            Printer.getPrinter().switchItem();
        }
    }

    @Shadow private static int nextPickSlotIndex;

    @Shadow @Final private static List<Integer> PICK_BLOCKABLE_SLOTS;

    /**
     * @author BlinkWhite
     * @reason 去除优先选择目前已选择的槽位
     */
    @Overwrite
    private static int getPickBlockTargetSlot(PlayerEntity player) {
        if (PICK_BLOCKABLE_SLOTS.isEmpty()) {
            return -1;
        }

        int slotNum;

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size()) {
            nextPickSlotIndex = 0;
        }

        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i) {
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size()) {
                nextPickSlotIndex = 0;
            }

            if (canPickToSlot(player.getInventory(), slotNum)) {
                return slotNum;
            }
        }

        return -1;
    }
}
