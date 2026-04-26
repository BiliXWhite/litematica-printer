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
    private static final float INSTANT_MINE_THRESHOLD = 20.0F;
    private static final float FAST_INSTANT_MINE_THRESHOLD = 45.0F;
    private static String lastInstantMineDebugLine;
    private static long cachedInstantMineTick = Long.MIN_VALUE;
    private static BedrockInstantMineInfo cachedInstantMineInfo;

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
        BedrockInstantMineInfo info = getBestInstantMineInfo(player);
        writeInstantMineDebug(player, info);
        if (!canInstantMinePiston(info)) {
            BedrockDebugLog.write("instantmine requirement failed threshold=" + formatSpeed(INSTANT_MINE_THRESHOLD));
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

    public static boolean shouldUseFastBreakProfile() {
        LocalPlayer player = CLIENT.player;
        BedrockInstantMineInfo info = player == null ? null : getBestInstantMineInfo(player);
        return isFastGroundedInstantMine(info, player);
    }

    private static boolean canInstantMinePiston(BedrockInstantMineInfo info) {
        return info != null && info.breakSpeed() > INSTANT_MINE_THRESHOLD;
    }

    private static void writeInstantMineDebug(LocalPlayer player, BedrockInstantMineInfo info) {
        if (info == null) {
            return;
        }
        String line = "instantmine check"
                + " slot=" + info.slot()
                + " item=" + BuiltInRegistries.ITEM.getKey(info.stack().getItem())
                + " speed=" + formatSpeed(info.breakSpeed())
                + " threshold=" + formatSpeed(INSTANT_MINE_THRESHOLD)
                + " instant=" + info.instantMine()
                + " profile=" + getInstantMineProfile(info, player)
                + " efficiency=" + info.efficiencyLevel()
                + " haste=" + info.hasteLevel()
                + " fatigue=" + info.fatigueLevel()
                + " onGround=" + player.onGround();
        if (!line.equals(lastInstantMineDebugLine)) {
            lastInstantMineDebugLine = line;
            BedrockDebugLog.write(line);
        }
    }

    private static BedrockInstantMineInfo getBestInstantMineInfo(LocalPlayer player) {
        if (CLIENT.level != null) {
            long currentTick = CLIENT.level.getGameTime();
            if (cachedInstantMineTick == currentTick) {
                return cachedInstantMineInfo;
            }
            cachedInstantMineTick = currentTick;
            cachedInstantMineInfo = computeBestInstantMineInfo(player);
            return cachedInstantMineInfo;
        }
        return computeBestInstantMineInfo(player);
    }

    private static BedrockInstantMineInfo computeBestInstantMineInfo(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        BedrockInstantMineInfo best = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = getBlockBreakingSpeed(player, Blocks.PISTON.defaultBlockState(), stack);
            int efficiencyLevel = getEfficiencyLevel(stack);
            int hasteLevel = MobEffectUtil.hasDigSpeed(player) ? MobEffectUtil.getDigSpeedAmplification(player) + 1 : 0;
            int fatigueLevel = player.hasEffect(MobEffects.MINING_FATIGUE) ? player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier() + 1 : 0;
            BedrockInstantMineInfo current = new BedrockInstantMineInfo(slot, stack, speed, speed > INSTANT_MINE_THRESHOLD, efficiencyLevel, hasteLevel, fatigueLevel);
            if (best == null || current.breakSpeed() > best.breakSpeed()) {
                best = current;
            }
        }
        return best;
    }

    private static boolean isFastGroundedInstantMine(BedrockInstantMineInfo info, LocalPlayer player) {
        return player != null
                && player.onGround()
                && info != null
                && info.breakSpeed() >= FAST_INSTANT_MINE_THRESHOLD;
    }

    private static String getInstantMineProfile(BedrockInstantMineInfo info, LocalPlayer player) {
        if (isFastGroundedInstantMine(info, player)) {
            return "fast";
        }
        if (canInstantMinePiston(info)) {
            return "baseline";
        }
        return player != null && player.onGround() ? "blocked" : "airborne";
    }

    private static String formatSpeed(float speed) {
        return String.format("%.3f", speed);
    }

    private static int getEfficiencyLevel(ItemStack itemStack) {
        //#if MC > 12006
        for (var entry : itemStack.getEnchantments().entrySet()) {
            Optional<net.minecraft.resources.ResourceKey<Enchantment>> key = entry.getKey().unwrapKey();
            if (key.isPresent() && key.get() == Enchantments.EFFICIENCY) {
                return EnchantmentHelper.getItemEnchantmentLevel(entry.getKey(), itemStack);
            }
        }
        return 0;
        //#else
        //$$ return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY, itemStack);
        //#endif
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

    private record BedrockInstantMineInfo(
            int slot,
            ItemStack stack,
            float breakSpeed,
            boolean instantMine,
            int efficiencyLevel,
            int hasteLevel,
            int fatigueLevel
    ) {
    }
}
