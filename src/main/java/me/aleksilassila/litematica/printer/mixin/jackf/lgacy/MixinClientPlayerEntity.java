package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

//#if MC < 12002
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.printerMemoryAdding;
import static me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils.handleItemsFromScreen;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Shadow @Final protected MinecraftClient client;
    @Inject(at = @At("HEAD"), method = "closeScreen")
    public void closeScreen(CallbackInfo ci) {
        BlockPos pos = MemoryUtils.getLatestPos();
        if(LitematicaMixinMod.INVENTORY.getBooleanValue() &&
                (LitematicaMixinMod.PRINT_MODE.getBooleanValue() || LitematicaMixinMod.PRINT.getKeybind().isPressed() || printerMemoryAdding) &&
                pos != null){
            if(!client.player.currentScreenHandler.equals(client.player.playerScreenHandler)){
                handleItemsFromScreen(client.player.currentScreenHandler);
            }
        }
    }
}
//#endif