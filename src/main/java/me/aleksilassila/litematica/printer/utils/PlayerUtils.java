package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.RadiusShapeType;
import me.aleksilassila.litematica.printer.enums.SelectionType;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("EnhancedSwitchMigration")
public class PlayerUtils {
    private static final Minecraft client = Minecraft.getInstance();

    public static Optional<LocalPlayer> getPlayer() {
        return Optional.ofNullable(client.player);
    }

    public static Abilities getAbilities(LocalPlayer playerEntity) {
        return playerEntity.getAbilities();
    }

    public static double getInteractionRange(double defaultRange) {
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

    public static boolean isWithinBlockInteractionRange(LocalPlayer player, BlockPos blockPos, double additionalRange) {
        double blockPosX = blockPos.getX();
        double blockPosY = blockPos.getY();
        double blockPosZ = blockPos.getZ();
        double eyePosX = player.getX();
        double eyePosZ = player.getZ();
        //#if MC > 11802
        double eyePosY = player.getEyeY();
        //#else
        //$$ double eyePosY = player.getY() + 1.5;
        //#endif
        double distance = getInteractionRange(5) + additionalRange;
        //#if MC > 12006
        double dx = Math.max(Math.max(blockPosX - eyePosX, eyePosX - (blockPosX + 1)), 0);
        double dy = Math.max(Math.max(blockPosY - eyePosY, eyePosY - (blockPosY + 1)), 0);
        double dz = Math.max(Math.max(blockPosZ - eyePosZ, eyePosZ - (blockPosZ + 1)), 0);
        return dx * dx + dy * dy + dz * dz < distance * distance;
        //#else
        //$$ double dx = eyePosX - blockPosX + 0.5;
        //$$ double dy = eyePosY - blockPosY + 0.5;
        //$$ double dz = eyePosZ - blockPosZ + 0.5;
        //$$ return dx * dx + dy * dy + dz * dz <= distance * distance;
        //#endif
    }

    // 球面（欧几里得距离）
    public static boolean isWithinWorkInteractedEuclideanRange(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) <= range * range;
    }

    public static boolean isWithinWorkInteractedManhattanRange(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        Vec3 eyePos = player.getEyePosition();
        BlockPos eyeBlockPos = new BlockPos((int) Math.floor(eyePos.x), (int) Math.floor(eyePos.y), (int) Math.floor(eyePos.z));
        int dx = Math.abs(blockPos.getX() - eyeBlockPos.getX());
        int dy = Math.abs(blockPos.getY() - eyeBlockPos.getY());
        int dz = Math.abs(blockPos.getZ() - eyeBlockPos.getZ());
        return dx + dy + dz <= range;
    }

    public static boolean isWithinWorkInteractedCubeRange(BlockPos blockPos, double range) {
        LocalPlayer player = client.player;
        if (player == null || blockPos == null) return false;
        Vec3 eyePos = player.getEyePosition();
        BlockPos eyeBlockPos = new BlockPos((int) Math.floor(eyePos.x), (int) Math.floor(eyePos.y), (int) Math.floor(eyePos.z));
        int dx = Math.abs(blockPos.getX() - eyeBlockPos.getX());
        int dy = Math.abs(blockPos.getY() - eyeBlockPos.getY());
        int dz = Math.abs(blockPos.getZ() - eyeBlockPos.getZ());
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

    public static boolean canInteracted(BlockPos blockPos) {
        if (ConfigUtils.client.player == null || blockPos == null) return false;

        if (Configs.Core.USE_REACH_DISTANCE.getBooleanValue()) {
            return isWithinBlockInteractionRange(ConfigUtils.client.player, blockPos, 0F);
        }

        double workRange = ConfigUtils.getWorkRange();
        if (Configs.Core.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType radiusShapeType) {
            return switch (radiusShapeType) {
                case SPHERE -> isWithinWorkInteractedEuclideanRange(blockPos, workRange);
                case OCTAHEDRON -> isWithinWorkInteractedManhattanRange(blockPos, workRange);
                case CUBE -> isWithinWorkInteractedCubeRange(blockPos, workRange);
            };
        }
        return isWithinWorkInteractedEuclideanRange(blockPos, workRange);
    }

    public static boolean isPositionInSelectionRange(Player player, @NotNull BlockPos pos, ConfigOptionList selectionTypeConfig) {
        if (player == null || selectionTypeConfig == null) {
            return false;
        }
        if (!(selectionTypeConfig.getOptionListValue() instanceof SelectionType selectionType)) {
            return false;
        }
        return switch (selectionType) {
            case LITEMATICA_RENDER_LAYER -> LitematicaUtils.isPositionWithinRange(pos);
            case LITEMATICA_SELECTION_BELOW_PLAYER -> pos.getY() <= Math.floor(player.getY());
            case LITEMATICA_SELECTION_ABOVE_PLAYER -> pos.getY() >= Math.ceil(player.getY());
            default -> true;
        };
    }
}
