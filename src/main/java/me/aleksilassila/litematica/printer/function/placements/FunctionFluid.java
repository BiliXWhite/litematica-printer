package me.aleksilassila.litematica.printer.function.placements;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockCooldownType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.function.FunctionPlacement;
import me.aleksilassila.litematica.printer.printer.BlockCooldownManager;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FunctionFluid extends FunctionPlacement {
    private List<String> fillBlocks = new ArrayList<>();
    private List<Item> fillItems = new ArrayList<>();

    private List<String> fluidBlocks = new ArrayList<>();
    private List<Fluid> fluids = List.of(new Fluid[0]);

    private @Nullable BlockPos blockPos;

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.FLUID;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.Core.FLUID;
    }

    public @Nullable BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean canIterationTest(Printer printer, ClientLevel level, LocalPlayer player, BlockPos pos) {
        if (pos != null) {
            if (!PrinterUtils.isPositionInSelectionRange(player, pos, Configs.Fluid.FLUID_SELECTION_TYPE)) {
                return false;
            }
            if (BlockCooldownManager.INSTANCE.isOnCooldown(BlockCooldownType.FLUID, blockPos)) {
                return false;
            }
        }
        // 填充方块
        List<String> fileBlocks = Configs.Fluid.FLUID_REPLACE_BLOCK_LIST.getStrings();
        if (!fileBlocks.equals(fillBlocks)) {
            fillBlocks = new ArrayList<>(fileBlocks);
            if (fileBlocks.isEmpty()) {
                return false;
            }
            fillItems = new ArrayList<>();
            for (String itemName : fillBlocks) {
                List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> FilterUtils.matchName(itemName, new ItemStack(item))).toList();
                fillItems.addAll(list);
            }
        }
        // 流体方块
        List<String> fluidBlocks = Configs.Fluid.FLUID_LIST.getStrings();
        if (!fluidBlocks.equals(this.fluidBlocks)) {
            this.fluidBlocks = new ArrayList<>(fluidBlocks);
            if (fluidBlocks.isEmpty()) {
                return false;
            }
            fluids = new ArrayList<>();
            for (String itemName : this.fluidBlocks) {
                List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> FilterUtils.matchName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                fluids.addAll(list);
            }
        }
        return true;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        if (!canIterationTest(printer, level, player, blockPos)) {
            return;
        }
        int placeBlocksPerTick = Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
        boolean loop = true;
        while (loop && (blockPos = getBoxBlockPos()) != null) {
            if (Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue() != 0 && placeBlocksPerTick == 0) {
                loop = false;
            }
            if (BlockCooldownManager.INSTANCE.isOnCooldown(BlockCooldownType.FLUID, blockPos)) {
                loop = false;
            }
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Fluid.FLUID_SELECTION_TYPE)) {
                BlockCooldownManager.INSTANCE.setCooldown(BlockCooldownType.FLUID, blockPos, 8);
                continue;
            }
            FluidState fluidState = level.getBlockState(blockPos).getFluidState();
            if (fluids.contains(fluidState.getType())) {
                if (!Configs.Fluid.FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource())
                    continue;
                if (!printer.switchToItems(player, fillItems.toArray(new Item[0]))) {
                    continue;
                }
                new PlacementGuide.Action().queueAction(printer.queue, blockPos, Direction.UP, false);
                printer.queue.sendQueue(player);
                if (Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue() != 0) {
                    placeBlocksPerTick--;
                }
            }
            BlockCooldownManager.INSTANCE.setCooldown(BlockCooldownType.FLUID, blockPos, ConfigUtils.getPlaceCooldown());
        }
    }
}
