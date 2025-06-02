package me.aleksilassila.litematica.printer.mixin.masa;


import com.google.common.collect.Lists;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.getEmptyPickBlockableHotbarSlot;
import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.getPickBlockTargetSlot;
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

    @Inject(at = @At("HEAD"),method = "setPickedItemToHand(ILnet/minecraft/item/ItemStack;Lnet/minecraft/client/MinecraftClient;)V", cancellable = true)
    private static void setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc, CallbackInfo ci) {
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();
        var usePacket = LitematicaMixinMod.PLACE_USE_PACKET.getBooleanValue();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot))
        {
            if (usePacket)
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(sourceSlot));
            inventory.selectedSlot = sourceSlot;
        }
        else
        {
            int hotbarSlot = sourceSlot;

            if (sourceSlot == -1 || !PlayerInventory.isValidHotbarIndex(sourceSlot))
            {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (hotbarSlot == -1)
            {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1)
            {
                if (usePacket)
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                inventory.selectedSlot = hotbarSlot;

                if (player.isCreative())
                {
                    //#if MC <= 12101
                    //$$player.getInventory().addPickBlock(stack.copy());
                    //#else
                    player.getInventory().swapStackWithHotbar(stack.copy());
                    //#endif
                    mc.interactionManager.clickCreativeStack(player.getMainHandStack(), 36 + player.getInventory().selectedSlot);
                    return;
                } else {
                    int slot1 = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.playerScreenHandler, stack.copy(), true);
                    if (slot1 != -1) {
                        // 使用数据包或普通点击方式交换槽位中的物品
                        if (usePacket) {
                            Int2ObjectMap<ItemStack> snapshot = new Int2ObjectOpenHashMap<>();
                            DefaultedList<Slot> slots = player.currentScreenHandler.slots;
                            int totalSlots = slots.size();
                            List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
                            for (Slot slotItem : slots) {
                                copies.add(slotItem.getStack().copy());
                            }
                            for (int j = 0; j < totalSlots; j++) {
                                ItemStack original = copies.get(j);
                                ItemStack current = slots.get(j).getStack();
                                if (!ItemStack.areEqual(original, current)) {
                                    snapshot.put(j, current.copy());
                                }
                            }
                            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                    player.playerScreenHandler.syncId,
                                    player.currentScreenHandler.getRevision(),
                                    slot1,
                                    hotbarSlot,
                                    SlotActionType.SWAP,
                                    stack.copy(),
                                    snapshot));
                            player.playerScreenHandler.onSlotClick(sourceSlot, hotbarSlot, SlotActionType.SWAP, player);
                            Printer.swapSlotDelay = LitematicaMixinMod.SWAP_ITEM_DELAY.getIntegerValue();
                        } else {
                            mc.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, hotbarSlot, SlotActionType.SWAP, player);
                        }
                    }
                }

                WorldUtils.setEasyPlaceLastPickBlockTime();
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }
        ci.cancel();
    }


}
