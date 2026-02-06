package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockCooldownType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.printer.Action;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.BlockCooldownManager;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

public class FluidHandler extends ClientPlayerTickHandler {
    private List<String> fillBlocks = new ArrayList<>();
    private List<Item> fillItems = new ArrayList<>();

    private List<String> fluidBlocks = new ArrayList<>();
    private List<Fluid> fluids = List.of(new Fluid[0]);

    public FluidHandler() {
        super("fluid", PrintModeType.FLUID, Configs.Core.FLUID, Configs.Fluid.FLUID_SELECTION_TYPE, true);
    }

    @Override
    protected int getTickInterval() {
        return Configs.Placement.PLACE_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxEffectiveExecutionsPerTick() {
        return Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    protected void preprocess() {
        // 填充方块
        List<String> fileBlocks = Configs.Fluid.FLUID_REPLACE_BLOCK_LIST.getStrings();
        if (!fileBlocks.equals(fillBlocks)) {
            fillBlocks = new ArrayList<>(fileBlocks);
            if (!fileBlocks.isEmpty()) {
                fillItems = new ArrayList<>();
                for (String itemName : fillBlocks) {
                    List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> FilterUtils.matchName(itemName, new ItemStack(item))).toList();
                    fillItems.addAll(list);
                }
            }
        }
        // 流体方块
        List<String> fluidBlocks = Configs.Fluid.FLUID_LIST.getStrings();
        if (!fluidBlocks.equals(this.fluidBlocks)) {
            this.fluidBlocks = new ArrayList<>(fluidBlocks);
            if (!fluidBlocks.isEmpty()) {
                fluids = new ArrayList<>();
                for (String itemName : this.fluidBlocks) {
                    List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> FilterUtils.matchName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                    fluids.addAll(list);
                }
            }
        }
    }

    @Override
    protected boolean canExecuteIteration() {
        return !fillItems.isEmpty() && !fluidBlocks.isEmpty();
    }

    @Override
    public boolean canIterationBlockPos(BlockPos blockPos) {
        if (!isOpenHandler && !switchItem()) {
            return false;
        }
        return this.isBlockPosOnCooldown(blockPos);
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        FluidState fluidState = level.getBlockState(blockPos).getFluidState();
        if (fluids.contains(fluidState.getType())) {
            if (!Configs.Fluid.FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource()) {
                return;
            }
            if (!InventoryUtils.switchToItems(player, fillItems.toArray(new Item[0]))) {
                return;
            }
            new Action().queueAction(blockPos, Direction.UP, false);
            ActionManager.INSTANCE.sendQueue(player);
        }
        BlockCooldownManager.INSTANCE.setCooldown(BlockCooldownType.FLUID, blockPos, ConfigUtils.getPlaceCooldown());
    }
}
