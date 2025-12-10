package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.State;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;

public class FunctionFluidMode extends FunctionModeBase {
    List<String> fluidBlocklist = new ArrayList<>();
    List<String> fluidList = new ArrayList<>();
    public HashSet<Item> fluidModeItemList = new HashSet<>();
    public HashSet<Fluid> fluidModeList = new HashSet<>();
    public Item[] fluidItemsArray = new Item[0];
    public Fluid[] fluidArray = new Fluid[0];

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
        if (!InitHandler.FLUID_BLOCK_LIST.getStrings().equals(fluidBlocklist)) {
            fluidBlocklist.clear();
            fluidBlocklist.addAll(InitHandler.FLUID_BLOCK_LIST.getStrings());
            if (InitHandler.FLUID_BLOCK_LIST.getStrings().isEmpty())
                return;
            fluidModeItemList.clear();
            for (String itemName : fluidBlocklist) {
                List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                fluidModeItemList.addAll(list);
            }
            fluidItemsArray = fluidModeItemList.toArray(new Item[0]);
        }

        if (!InitHandler.FLUID_LIST.getStrings().equals(fluidList)) {
            fluidList.clear();
            fluidList.addAll(InitHandler.FLUID_LIST.getStrings());
            if (InitHandler.FLUID_LIST.getStrings().isEmpty())
                return;
            fluidModeList.clear();
            for (String itemName : fluidList) {
                List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> equalsBlockName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                fluidModeList.addAll(list);
            }
            fluidArray = fluidModeList.toArray(new Fluid[0]);
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
            if (Arrays.asList(fluidArray).contains(fluidState.getType())) {
                if (!InitHandler.FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource())
                    continue;
                if (printer.switchToItems(client.player, fluidItemsArray)) {
                    new PlacementGuide.Action().queueAction(printer.queue, pos, Direction.UP, false);
                    if (printer.tickRate == 0) {
                        printer.queue.sendQueue(client.player);
                        if (InitHandler.BLOCKS_PER_TICK.getIntegerValue() != 0) {
                            printer.printerWorkingCountPerTick--;
                        }
                        continue;
                    }
                    return;
                }
            }
        }
    }
}
