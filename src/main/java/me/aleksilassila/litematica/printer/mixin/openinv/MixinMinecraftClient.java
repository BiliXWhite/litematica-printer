//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.LitematicaMixinMod.INVENTORY;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.QUICKSHULKER;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MixinMinecraftClient {
    @Shadow
    public ClientPlayerEntity player;

    @Inject(method = {"setScreen"}, at = {@At(value = "HEAD")}, cancellable = true)
    public void setScreen(@Nullable Screen screen, CallbackInfo ci) {
        if(closeScreen > 0 && /*screen != null &&*/ screen instanceof HandledScreen<?>){
            closeScreen--;
            ci.cancel();
        }
    }
    //鼠标中键从打印机库存或通过快捷濳影盒 取出对应物品
    @WrapOperation(method = "doItemPick",at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSlotWithStack(Lnet/minecraft/item/ItemStack;)I" ))
    private int doItemPick(PlayerInventory instance, ItemStack stack, Operation<Integer> original) {
        int slotWithStack = original.call(instance, stack);
        if(!player.getAbilities().creativeMode && (INVENTORY.getBooleanValue() || QUICKSHULKER.getBooleanValue()) && slotWithStack == -1){
            Item item = stack.getItem();
            Printer.remoteItem.add(item);
            Printer.getPrinter().switchItem();
            return -1;
        }
        return slotWithStack;
    }
}
