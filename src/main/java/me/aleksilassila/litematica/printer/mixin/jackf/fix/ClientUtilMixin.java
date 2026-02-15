package me.aleksilassila.litematica.printer.mixin.jackf.fix;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.utils.ModLoadStatus;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#endif

@Mixin(value = ClientUtil.class, remap = false)
public class ClientUtilMixin {
    @Inject(at = @At("HEAD"),method = "CheckAndSend")
    private static void CheckAndSend(ItemStack stack, int slot, CallbackInfoReturnable<Boolean> cir) {
        //远程取物时再打开濳影盒会将濳影盒内的物品保存到打开的容器..
        PlayerUtils.getPlayer().ifPresent(player ->{
            if(ModLoadStatus.isLoadChestTrackerLoaded()){
                //#if MC >= 12001
                MemoryUtils.saveMemory(player.containerMenu);
                OpenInventoryPacket.reSet();
                //#endif
            }
        });
    }
}