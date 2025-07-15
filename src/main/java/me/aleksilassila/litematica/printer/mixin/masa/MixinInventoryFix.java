package me.aleksilassila.litematica.printer.mixin.masa;

import fi.dy.masa.litematica.util.InventoryUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(InventoryUtils.class)
public interface MixinInventoryFix {
    @Invoker("getPickBlockTargetSlot")
    static int getPickBlockTargetSlot(PlayerEntity player){
        return -1;
    }

    @Invoker("getEmptyPickBlockableHotbarSlot")
    static int getEmptyPickBlockableHotbarSlot(PlayerInventory inventory){
        return -1;
    }

    @Invoker("canPickToSlot")
    static boolean canPickToSlot(PlayerInventory inventory, int slot) {
        return false;
    }
}