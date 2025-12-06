package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryDatabase;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.key;

@Mixin(ChestTracker.class)
public class MixinChestTracker {
    @Inject(at = @At("TAIL"),method = "searchForItem")
    private static void searchForItem(ItemStack stack, CallbackInfo ci) {
        if(!InitHandler.CLOUD_INVENTORY.getBooleanValue() || key != null) return;
        MemoryDatabase database = MemoryDatabase.getCurrent();
        if (database != null) {
            int num = 0;
            for (ResourceLocation dimension : database.getDimensions()) {
                if(ZxyUtils.currWorldId == num){
                    for (Memory item : database.findItems(stack, dimension)) {
                        red.jackf.chesttracker.memory.MemoryUtils.setLatestPos(item.getPosition());
                        OpenInventoryPacket.sendOpenInventory(item.getPosition(), ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension));
                        return;
                    }
                }else {
                    num++;
                }
            }
            for (ResourceLocation dimension : database.getDimensions()) {
                for (Memory item : database.findItems(stack, dimension)) {
                    red.jackf.chesttracker.memory.MemoryUtils.setLatestPos(item.getPosition());
                    OpenInventoryPacket.sendOpenInventory(item.getPosition(), ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension));
                    return;
                }
            }
        }
    }
}
