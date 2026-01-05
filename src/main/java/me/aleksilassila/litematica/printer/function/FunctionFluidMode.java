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

import java.util.*;

public class FunctionFluidMode extends FunctionModeBase {
    private List<String> fillBlocks = new ArrayList<>();
    private List<Item> fillItems = new ArrayList<>();

    private List<String> fluidBlocks = new ArrayList<>();
    private List<Fluid> fluis = List.of(new Fluid[0]);

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.FLUID;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.Hotkeys.FLUID;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        // 填充方块
        List<String> fileBlocks = Configs.General.FLUID_BLOCK_LIST.getStrings();
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
        List<String> fluidBlocks = Configs.General.FLUID_LIST.getStrings();
        if (!fluidBlocks.equals(this.fluidBlocks)) {
            this.fluidBlocks = new ArrayList<>(fluidBlocks);
            if (fluidBlocks.isEmpty()) {
                return;
            }
            fluis = new ArrayList<>();
            for (String itemName : this.fluidBlocks) {
                List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> FilterUtils.matchName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                fluis.addAll(list);
            }
        }

        BlockPos pos;
        while ((pos = printer.getBlockPos()) != null) {
            if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0 && printer.printerWorkingCountPerTick == 0) {
                return;
            }
            if (!PrinterUtils.isPositionInSelectionRange(player, pos, Configs.Put.FLUID_SELECTION_TYPE)) {
                continue;
            }
            printer.placeCooldownList.put(pos, Configs.General.PLACE_COOLDOWN.getIntegerValue());
            FluidState fluidState = level.getBlockState(pos).getFluidState();
            if (fluis.contains(fluidState.getType())) {
                if (!Configs.Put.FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource())
                    continue;
                if (printer.switchToItems(player, fillItems.toArray(new Item[0]))) {
                    new PlacementGuide.Action().queueAction(printer.queue, pos, Direction.UP, false);
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
