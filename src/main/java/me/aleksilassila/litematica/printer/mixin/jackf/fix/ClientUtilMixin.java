package me.aleksilassila.litematica.printer.mixin.jackf.fix;

import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#endif

@Mixin(ClientUtil.class)
public class ClientUtilMixin {
    @Inject(at = @At("HEAD"),method = "CheckAndSend")
    private static void CheckAndSend(ItemStack stack, int slot, CallbackInfoReturnable<Boolean> cir) {
        //保存一下当前打开的容器
        ZxyUtils.getPlayer().ifPresent(player ->{
            if(Statistics.loadChestTracker){
                //#if MC >= 12001
                MemoryUtils.saveMemory(player.currentScreenHandler);
                OpenInventoryPacket.reSet();
                //#endif
//                player.closeScreen();
            }
        });
    }
}
