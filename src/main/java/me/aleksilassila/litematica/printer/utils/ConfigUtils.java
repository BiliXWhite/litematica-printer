package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.options.ConfigOptionList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.FillModeFacingType;
import me.aleksilassila.litematica.printer.enums.SelectionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();

    public static boolean isPrinterEnable() {
        return Configs.Core.WORK_SWITCH.getBooleanValue();
    }

    public static boolean isPrintEnabled() {
        return Configs.Print.ENABLED.getBooleanValue();
    }

    public static boolean isMineEnabled() {
        return Configs.Mine.ENABLED.getBooleanValue();
    }

    public static boolean isFillEnabled() {
        return Configs.Fill.ENABLED.getBooleanValue();
    }

    public static boolean isFluidEnabled() {
        return Configs.Fluid.ENABLED.getBooleanValue();
    }

    public static boolean isBedrockEnabled() {
        return Configs.Bedrock.ENABLED.getBooleanValue();
    }

    public static int getPlaceCooldown() {
        return Configs.Placement.PLACE_COOLDOWN.getIntegerValue();
    }

    public static int getBreakCooldown() {
        return Configs.Break.BREAK_COOLDOWN.getIntegerValue();
    }

    public static int getWorkRange() {
        return (int) Configs.Core.WORK_RANGE.getDoubleValue();
    }

    public static double getEffectiveRange() {
        double configRange = Configs.Core.WORK_RANGE.getDoubleValue();
        if (configRange <= 0) {
            return PlayerUtils.getInteractionRange(4.5);
        }
        return configRange;
    }

    public static boolean isPositionInSelectionRange(@Nullable LocalPlayer player, @Nullable BlockPos pos,
                                                     @Nullable ConfigOptionList selectionConfig) {
        if (pos == null) return false;
        // 渲染层在扫描框构建时对所有模块生效，因此异步候选也必须全局复查
        if (!LitematicaUtils.isPositionWithinRange(pos)) return false;
        if (selectionConfig == null) return true;
        if (!(selectionConfig.getOptionListValue() instanceof SelectionType selectionType)) return true;
        return switch (selectionType) {
            case LITEMATICA_SELECTION, LITEMATICA_RENDER_LAYER -> true;
            case LITEMATICA_SELECTION_BELOW_PLAYER -> player != null && pos.getY() <= Math.floor(player.getY());
            case LITEMATICA_SELECTION_ABOVE_PLAYER -> player != null && pos.getY() >= Math.ceil(player.getY());
        };
    }

    public static Direction getFillModeFacing() {
        if (Configs.Fill.FILL_BLOCK_FACING.getOptionListValue() instanceof FillModeFacingType fillModeFacingType) {
            return switch (fillModeFacingType) {
                case DOWN -> Direction.DOWN;
                case UP -> Direction.UP;
                case WEST -> Direction.WEST;
                case EAST -> Direction.EAST;
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
                default -> null;
            };
        }
        return null;
    }
}