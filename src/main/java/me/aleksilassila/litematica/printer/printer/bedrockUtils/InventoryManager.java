package me.aleksilassila.litematica.printer.printer.bedrockUtils;

//import net.fabricmc.fabric.api.event.client.player.ClientPickBlockCallback;
//import net.minecraft.client.MinecraftClient;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryManager {
    public static void refresh()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler != null && mc.player != null)
        {
//            ItemStack uniqueItem = new ItemStack(Items.STONE);
//            uniqueItem.getOrCreateTag().putDouble("force_resync", Double.NaN);  // Tags with NaN are not equal
//			networkHandler.sendPacket(new ClickWindowC2SPacket(
//					mc.player.container.syncId,
//					//#if MC >= 11700
//					//$$ mc.player.currentScreenHandler.getRevision(),
//					//#endif
//					-999, 2,
//					SlotActionType.QUICK_CRAFT,
//					uniqueItem,
//
//					//#if MC >= 11700
//					//$$ new Int2ObjectOpenHashMap<>()
//					//#else
//					mc.player.container.getNextActionId(mc.player.inventory)
//					//#endif
//			));
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, -999, 2, SlotActionType.QUICK_CRAFT, mc.player);

//			InfoUtils.printActionbarMessage("tweakermore.impl.refreshInventory.refreshed");
        }
    }
    static int i = 0;
    public static boolean switchToItem(ItemConvertible item) {

//        Item tm;
//        if (item instanceof Item) {
//            tm = (Item) item;
//        } else {
//            tm = item.asItem();
//        }
//        Printer.getPrinter().switchToItems(MinecraftClient.getInstance().player,new Item[]{tm});

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerInventory playerInventory = minecraftClient.player.getInventory();

        int i = playerInventory.getSlotWithStack(new ItemStack(item));

        if ("diamond_pickaxe".equals(item.toString())) {
            i = getEfficientTool(playerInventory);
        }
        if (i != -1) {

            if (PlayerInventory.isValidHotbarIndex(i)) {
                playerInventory.selectedSlot = i;
            } else {
                minecraftClient.interactionManager.pickFromInventory(i);
            }
            minecraftClient.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot));
            refresh();
            return true;
        }

        return false;
    }

    private static int getEfficientTool(PlayerInventory playerInventory) {
        for (int i = 0; i < playerInventory.main.size(); ++i) {
            if (getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) > 45f) {
                return i;
            }
        }
        return -1;
    }

    public static boolean canInstantlyMinePiston() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerInventory playerInventory = minecraftClient.player.getInventory();

        for (int i = 0; i < playerInventory.size(); i++) {
            if (getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) > 45f) {
                return true;
            }
        }
        return false;
    }

    private static float getBlockBreakingSpeed(BlockState block, int slot) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        ItemStack stack = player.getInventory().getStack(slot);

        float f = stack.getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
            ItemStack itemStack = player.getInventory().getStack(slot);
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k;
            switch (player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    k = 0.3F;
                    break;
                case 1:
                    k = 0.09F;
                    break;
                case 2:
                    k = 0.0027F;
                    break;
                case 3:
                default:
                    k = 8.1E-4F;
            }

            f *= k;
        }

        if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            f /= 5.0F;
        }

        if (!player.isOnGround()) {
            f /= 5.0F;
        }

        return f;
    }

    public static int getInventoryItemCount(ItemConvertible item) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerInventory playerInventory = minecraftClient.player.getInventory();
        return playerInventory.count(item.asItem());
    }

    public static String warningMessage() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (!"survival".equals(minecraftClient.interactionManager.getCurrentGameMode().getName())) {
            return "bedrockminer.fail.missing.survival";
        }

        if (InventoryManager.getInventoryItemCount(Blocks.PISTON) < 2) {
            return "bedrockminer.fail.missing.piston";
        }

        if (InventoryManager.getInventoryItemCount(Blocks.REDSTONE_TORCH) < 1) {
            return "bedrockminer.fail.missing.redstonetorch";
        }

        if (InventoryManager.getInventoryItemCount(Blocks.SLIME_BLOCK) < 1) {
            return "bedrockminer.fail.missing.slime";
        }

        if (!InventoryManager.canInstantlyMinePiston()) {
            return "bedrockminer.fail.missing.instantmine";
        }
        return null;
    }

}
