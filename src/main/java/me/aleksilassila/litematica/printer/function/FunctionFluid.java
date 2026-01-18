package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
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

public class FunctionFluid extends Function {
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
        return Configs.Hotkeys.FLUID;
    }

    public @Nullable BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        // 填充方块
        List<String> fileBlocks = Configs.FLUID.FLUID_BLOCK_LIST.getStrings();
        if (!fileBlocks.equals(fillBlocks)) {
            fillBlocks = new ArrayList<>(fileBlocks);
            if (fileBlocks.isEmpty()) {
                return;
            }
            fillItems = new ArrayList<>();
            for (String itemName : fillBlocks) {
                List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> FilterUtils.matchName(itemName, new ItemStack(item))).toList();
                fillItems.addAll(list);
            }
        }
        // 流体方块
        List<String> fluidBlocks = Configs.FLUID.FLUID_LIST.getStrings();
        if (!fluidBlocks.equals(this.fluidBlocks)) {
            this.fluidBlocks = new ArrayList<>(fluidBlocks);
            if (fluidBlocks.isEmpty()) {
                return;
            }
            fluids = new ArrayList<>();
            for (String itemName : this.fluidBlocks) {
                List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> FilterUtils.matchName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                fluids.addAll(list);
            }
        }
        boolean loop = true;
        while (loop && (blockPos = printer.getBlockPos()) != null) {
            if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0 && printer.printerWorkingCountPerTick == 0) {
                loop = false;
            }
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.FLUID.FLUID_SELECTION_TYPE)) {
                continue;
            }
            printer.placeCooldownList.put(blockPos, Configs.General.PLACE_COOLDOWN.getIntegerValue());
            FluidState fluidState = level.getBlockState(blockPos).getFluidState();
            if (fluids.contains(fluidState.getType())) {
                if (!Configs.FLUID.FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource())
                    continue;
                if (printer.switchToItems(player, fillItems.toArray(new Item[0]))) {
                    new PlacementGuide.Action().queueAction(printer.queue, blockPos, Direction.UP, false);
                    if (printer.tickRate == 0) {
                        printer.queue.sendQueue(player);
                        if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0) {
                            printer.printerWorkingCountPerTick--;
                        }
                        continue;
                    }
                    printer.queue.sendQueue(player);
                    return;
                }
            }
        }
    }
}
