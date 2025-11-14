package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;


import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryDatabase;
//#if MC < 11904
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
//#else
//$$ import net.minecraft.registry.RegistryKeys;
//#endif
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.key;
@Mixin(ChestTracker.class)
public class MixinChestTracker {
    @Inject(at = @At("TAIL"),method = "searchForItem")
    private static void searchForItem(ItemStack stack, CallbackInfo ci) {
        if(!LitematicaPrinterMod.CLOUD_INVENTORY.getBooleanValue() || key != null) return;
        MemoryDatabase database = MemoryDatabase.getCurrent();
        if (database != null) {
            int num = 0;
            for (Identifier dimension : database.getDimensions()) {
                if(ZxyUtils.currWorldId == num){
                    for (Memory item : database.findItems(stack, dimension)) {
                        red.jackf.chesttracker.memory.MemoryUtils.setLatestPos(item.getPosition());
                        OpenInventoryPacket.sendOpenInventory(item.getPosition(), RegistryKey.of(Registry.WORLD_KEY, dimension));
                        return;
                    }
                }else {
                    num++;
                }
            }
            for (Identifier dimension : database.getDimensions()) {
                for (Memory item : database.findItems(stack, dimension)) {
                    red.jackf.chesttracker.memory.MemoryUtils.setLatestPos(item.getPosition());
                    OpenInventoryPacket.sendOpenInventory(item.getPosition(), RegistryKey.of(Registry.WORLD_KEY, dimension));
                    return;
                }
            }
        }
    }
}
