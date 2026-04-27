package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class BedrockInventory {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private BedrockInventory() {
    }

    public static String warningMessage() {
        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.gameMode == null) {
            return "bedrockminer.fail.missing.survival";
        }
        if (CLIENT.gameMode.getPlayerMode().isCreative()) {
            return "bedrockminer.fail.missing.survival";
        }
        if (count(Items.PISTON) < 2) {
            return "bedrockminer.fail.missing.piston";
        }
        if (count(Items.REDSTONE_TORCH) < 1) {
            return "bedrockminer.fail.missing.redstonetorch";
        }
        if (count(Items.SLIME_BLOCK) < 1) {
            return "bedrockminer.fail.missing.slime";
        }
        if (!canInstantMinePiston(player)) {
            return "bedrockminer.fail.missing.instantmine";
        }
        return null;
    }

    public static boolean switchToItem(Item item) {
        LocalPlayer player = CLIENT.player;
        return player != null && InventoryUtils.switchToItems(player, new Item[]{item});
    }

    public static boolean switchToOffhand(Item item) {
        return InventoryUtils.setItemToOffhand(new ItemStack(item), CLIENT);
    }

    public static boolean hasAtLeast(Item item, int count) {
        return count(item) >= count;
    }

    private static int count(Item item) {
        LocalPlayer player = CLIENT.player;
        return player == null ? 0 : player.getInventory().countItem(item);
    }

    private static boolean canInstantMinePiston(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (getBlockBreakingSpeed(player, Blocks.PISTON.defaultBlockState(), inventory.getItem(slot)) > 20.0F) {
                return true;
            }
        }
        return false;
    }

    private static float getBlockBreakingSpeed(LocalPlayer player, BlockState blockState, ItemStack itemStack) {
        float speed = itemStack.getDestroySpeed(blockState);
        //#if MC > 12006
        if (speed > 1.0F) {
            for (var entry : itemStack.getEnchantments().entrySet()) {
                Optional<net.minecraft.resources.ResourceKey<Enchantment>> key = entry.getKey().unwrapKey();
                if (key.isPresent() && key.get() == Enchantments.EFFICIENCY) {
                    int level = EnchantmentHelper.getItemEnchantmentLevel(entry.getKey(), itemStack);
                    if (level > 0 && !itemStack.isEmpty()) {
                        speed += (float) (level * level + 1);
                    }
                }
            }
        }
        //#else
        //$$ if (speed > 1.0F) {
        //$$     int level = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY, itemStack);
        //$$     if (level > 0 && !itemStack.isEmpty()) {
        //$$         speed += (float) (level * level + 1);
        //$$     }
        //$$ }
        //#endif
        if (MobEffectUtil.hasDigSpeed(player)) {
            speed *= 1.0F + (float) (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }
        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            int amplifier = player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier();
            speed *= switch (amplifier) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }
        //#if MC > 12006
        AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (breakSpeed != null) {
            speed *= (float) breakSpeed.getValue();
        }
        //#endif
        if (!player.onGround()) {
            speed /= 5.0F;
        }
        return speed;
    }
}
