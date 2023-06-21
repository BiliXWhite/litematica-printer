package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryDatabase;

import java.util.Collection;


@Mixin(ChestTracker.class)
public class MixinChestTracker {

    @Inject(at = @At("TAIL"),method = "searchForItem")
    private static void searchForItem(ItemStack stack, CallbackInfo ci) {
        MemoryDatabase database = MemoryDatabase.getCurrent();


        if (database != null) {
            int num = 0;
            for (Identifier dimension : database.getDimensions()) {
                if(ZxyUtils.currWorldId == num){
                    for (Memory item : database.findItems(stack, dimension)) {
                        ZxyUtils.qw = true;
                        OpenInventoryPacket.sendOpenInventory(item.getPosition(), RegistryKey.of(RegistryKeys.WORLD,dimension));
                        return;
                    }
                }else {
                    num++;
                }
            }

            for (Identifier dimension : database.getDimensions()) {
                for (Memory item : database.findItems(stack, dimension)) {
                    ZxyUtils.qw = true;
                    OpenInventoryPacket.sendOpenInventory(item.getPosition(), RegistryKey.of(RegistryKeys.WORLD,dimension));
                    return;
                }
            }
        }
    }
}