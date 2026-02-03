//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.openinv;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus.closeScreen;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.lastNeedItemList;

//#if MC <= 12103
//$$ import net.minecraft.world.entity.player.Inventory;
//$$ import net.minecraft.world.item.ItemStack;
//#endif

@Environment(EnvType.CLIENT)
@Mixin({Minecraft.class})
public abstract class MixinMinecraftClient {
    @Shadow
    public LocalPlayer player;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Inject(method = {"setScreen"}, at = {@At(value = "HEAD")}, cancellable = true)
    public void setScreen(@Nullable Screen screen, CallbackInfo ci) {
        if(closeScreen > 0 && /*screen != null &&*/ screen instanceof AbstractContainerScreen<?>){
            closeScreen--;
            ci.cancel();
        }
    }

    //鼠标中键从打印机库存或通过快捷濳影盒 取出对应物品
    //#if MC > 12103
    @WrapOperation(method = "pickBlock",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handlePickItemFromBlock(Lnet/minecraft/core/BlockPos;Z)V"))
    private void doItemPick(MultiPlayerGameMode instance, BlockPos pos, boolean b, Operation<Void> original) {
        if(level == null) {
            original.call(instance, pos, b);
            return;
        }
        Item item = level.getBlockState(pos).getBlock().asItem();
        if (player.inventoryMenu.slots.stream().noneMatch(slot -> slot.getItem().getItem().equals(item)) &&
                !player.getAbilities().instabuild && (Configs.Core.CLOUD_INVENTORY.getBooleanValue() || Configs.Placement.QUICK_SHULKER.getBooleanValue())) {
            lastNeedItemList.add(item);
            InventoryUtils.switchItem();
            return;
        }
        original.call(instance, pos, b);
    }
    //TODO: 以下低版本mixin有错, 分析检查出来的, 待修复
    //#else
    //$$ @WrapOperation(method = "pickBlock",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I" ))
    //$$ private int doItemPick(Inventory instance, ItemStack stack, Operation<Integer> original) {
    //$$     int slotWithStack = original.call(instance, stack);
    //$$     if(!player.getAbilities().instabuild && (Configs.Core.CLOUD_INVENTORY.getBooleanValue() || Configs.Placement.QUICK_SHULKER.getBooleanValue()) && slotWithStack == -1){
    //$$         Item item = stack.getItem();
    //$$         lastNeedItemList.add(item);
    //$$         InventoryUtils.switchItem();
    //$$         return -1;
    //$$     }
    //$$     return slotWithStack;
    //$$ }
    //#endif

}