package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
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

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;

public class FunctionFluidMode extends FunctionModeBase {
    private List<String> fillBlocks = new ArrayList<>();
    private List<Item> fillItems = new ArrayList<>();

    private List<String> fluidBlocks = new ArrayList<>();
    private List<Fluid> fluis = List.of(new Fluid[0]);

    @Override
    public State.PrintModeType getPrintModeType() {
        return State.PrintModeType.FLUID;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return InitHandler.FLUID;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        Printer.getInstance().requiredState = null;
        // 填充方块
        List<String> fileBlocks = InitHandler.FLUID_BLOCK_LIST.getStrings();
        if (!fileBlocks.equals(fillBlocks)) {
            fillBlocks = new ArrayList<>(fileBlocks);
            if (fileBlocks.isEmpty()) {
                return;
            }
            fillItems = new ArrayList<>();
            for (String itemName : fillBlocks) {
                List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                fillItems.addAll(list);
            }
        }
        // 流体方块
        List<String> fluidBlocks = InitHandler.FLUID_LIST.getStrings();
        if (!fluidBlocks.equals(this.fluidBlocks)) {
            this.fluidBlocks = new ArrayList<>(fluidBlocks);
            if (fluidBlocks.isEmpty()) {
                return;
            }
            fluis = new ArrayList<>();
            for (String itemName : this.fluidBlocks) {
                List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> equalsBlockName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                fluis.addAll(list);
            }
        }

        BlockPos pos;
        while ((pos = printer.getBlockPos()) != null) {
            if (InitHandler.BLOCKS_PER_TICK.getIntegerValue() != 0 && printer.printerWorkingCountPerTick == 0)
                return;
            if (PrinterUtils.isLimitedByTheNumberOfLayers(pos))
                continue;
            if (!Printer.TempData.xuanQuFanWeiNei_p(pos))
                continue;
            // 跳过冷却中的位置
            if (Printer.getInstance().placeCooldownList.containsKey(pos))
                continue;
            Printer.getInstance().placeCooldownList.put(pos, InitHandler.PLACE_COOLDOWN.getIntegerValue());
            FluidState fluidState = level.getBlockState(pos).getFluidState();
            if (fluis.contains(fluidState.getType())) {
                if (!InitHandler.FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource())
                    continue;
                if (printer.switchToItems(player, fillItems.toArray(new Item[0]))) {
                    new PlacementGuide.Action().queueAction(printer.queue, pos, Direction.UP, false);
                    if (printer.tickRate == 0) {
                        printer.queue.sendQueue(player);
                        if (InitHandler.BLOCKS_PER_TICK.getIntegerValue() != 0) {
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
