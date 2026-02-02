package me.aleksilassila.litematica.printer.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;

public class PlayerUtils {
    private static final Minecraft client = Minecraft.getInstance();

    public static Optional<LocalPlayer> getPlayer() {
        return Optional.ofNullable(client.player);
    }

    public static Abilities getAbilities(LocalPlayer playerEntity) {
        return playerEntity.getAbilities();
    }

    public static double getPlayerBlockInteractionRange(double defaultRange) {
        //#if MC>=12005
        if (client.player != null) {
            return client.player.blockInteractionRange();
        }
        //#else
        //$$ if (client.gameMode != null) {
        //$$    return client.gameMode.getPickRange();
        //$$ }
        //#endif
        return defaultRange;
    }

    public static double getPlayerBlockInteractionRange() {
        boolean creative = client.gameMode != null && client.gameMode.getPlayerMode().isCreative();
        return getPlayerBlockInteractionRange(creative ? 5.0F : 4.5F);
    }

    public static boolean canInteractWithBlockAt(LocalPlayer player, BlockPos blockPos, double additionalRange) {
        double blockCenterX = blockPos.getX() + 0.5;
        double blockCenterY = blockPos.getY() + 0.5;
        double blockCenterZ = blockPos.getZ() + 0.5;
        double distance, eyeY;
        double eyeX = player.getX();
        double eyeZ = player.getZ();

        //#if MC >= 11900
        distance = getPlayerBlockInteractionRange() + additionalRange;
        eyeY = player.getEyeY();
        //#else
        //$$ // MC <= 11802
        //$$ distance = Math.max(getPlayerBlockInteractionRange(), 5) + additionalRange;
        //$$ eyeY = player.getY() + 1.5;
        //#endif

        double dx = eyeX - blockCenterX;
        double dy = eyeY - blockCenterY;
        double dz = eyeZ - blockCenterZ;
        return dx * dx + dy * dy + dz * dz < distance * distance;
    }

    // 球面（欧几里得距离）
    public static boolean canInteractedEuclidean(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) <= range * range;
    }

    // 八面体（曼哈顿距离）
    public static boolean canInteractedManhattan(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        BlockPos center = player.blockPosition();
        int dx = Math.abs(blockPos.getX() - center.getX());
        int dy = Math.abs(blockPos.getY() - center.getY());
        int dz = Math.abs(blockPos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }

    // 立方体（CUBE）：以玩家方块位置为中心
    public static boolean canInteractedCube(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        BlockPos center = player.blockPosition();
        int dx = Math.abs(blockPos.getX() - center.getX());
        int dy = Math.abs(blockPos.getY() - center.getY());
        int dz = Math.abs(blockPos.getZ() - center.getZ());
        return dx <= range && dy <= range && dz <= range;
    }


    public static float getDestroyProgress(LocalPlayer player, BlockState state, ItemStack itemStack) {
        float hardness = state.getBlock().defaultDestroyTime();
        if (hardness == -1.0F) {
            return 0.0F;
        } else {
            int i = player.hasCorrectToolForDrops(state) ? 30 : 100;
            return getBlockBreakingSpeed(player, state, itemStack) / hardness / (float) i;
        }
    }

    public static float getDestroyProgress(LocalPlayer player, BlockState state, boolean mainHand) {
        return getDestroyProgress(player, state, mainHand ? player.getMainHandItem() : player.getOffhandItem());
    }

    public static float getDestroyProgress(LocalPlayer player, BlockState state) {
        return getDestroyProgress(player, state, true);
    }

    /**
     * 获取当前物品能够破坏指定方块的破坏速度.
     *
     * @param blockState 要破坏的方块状态
     * @param itemStack  使用工具/物品破坏方块
     * @return 当前物品破坏该方块所需的时间（单位为 tick）
     */
    public static float getBlockBreakingSpeed(LocalPlayer player, BlockState blockState, ItemStack itemStack) {
        float f = itemStack.getDestroySpeed(blockState);

        //#if MC > 12006
        if (f > 1.0F) {
            for (Holder<Enchantment> enchantment : itemStack.getEnchantments().keySet()) {
                Optional<ResourceKey<Enchantment>> enchantmentKey = enchantment.unwrapKey();
                if (enchantmentKey.isPresent()) {
                    if (enchantmentKey.get() == Enchantments.EFFICIENCY) {
                        int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, itemStack);
                        if (level > 0 && !itemStack.isEmpty()) {
                            f += (float) (level * level + 1);
                        }
                    }
                }
            }
        }
        //#else
        //$$ if (f > 1.0F) {
        //$$     int level = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY, itemStack);
        //$$     if (level > 0 && !itemStack.isEmpty()) {
        //$$         f += (float)(level * level + 1);
        //$$     }
        //$$ }
        //#endif

        if (MobEffectUtil.hasDigSpeed(player)) {
            f *= 1.0F + (float) (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }

        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float g;
            switch (Objects.requireNonNull(player.getEffect(MobEffects.MINING_FATIGUE)).getAmplifier()) {
                case 0:
                    g = 0.3F;
                    break;
                case 1:
                    g = 0.09F;
                    break;
                case 2:
                    g = 0.0027F;
                    break;
                default:
                    g = 8.1E-4F;
                    break;
            }
            f *= g;
        }

        //#if MC > 12006
        f *= (float) player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (player.isEyeInFluid(FluidTags.WATER)) {
            AttributeInstance submergedMiningSpeed = player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
            if (submergedMiningSpeed != null) {
                f *= (float) submergedMiningSpeed.getValue();
            }
        }
        //#else
        //$$ if (player.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
        //$$     f /= 5.0F;
        //$$ }
        //#endif

        if (!player.onGround()) {
            f /= 5.0F;
        }
        return f;
    }
}
