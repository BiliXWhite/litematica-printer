package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import lombok.Getter;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockPrintState;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.Handlers;
import me.aleksilassila.litematica.printer.printer.BlockContext;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class GuiHandler extends ClientPlayerTickHandler {
    private int workProgressTotalCount;
    private int workProgressFinishedCount;
    @Getter
    private float workProgress;

    public GuiHandler() {
        super("gui", null, Configs.Core.DEBUG_OUTPUT, null, true);
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {

        if (ConfigUtils.isPrintMode()) {
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Print.PRINT_SELECTION_TYPE)) {
                return;
            }
            WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
            if (schematic == null) {
                workProgress = 0.0f;
            }
            BlockContext context = new BlockContext(client, level, schematic, blockPos);
            if (context.requiredState.isAir()) {
                return;
            }
            if (BlockPrintState.get(context) == BlockPrintState.CORRECT) {
                workProgressFinishedCount++;
            }
        }
        if (isFluidMode()) {
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Fluid.FLUID_SELECTION_TYPE)) {
                return;
            }
            if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
                workProgressFinishedCount++;
            }
        }

        if (isFillMode()) {
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Fill.FILL_SELECTION_TYPE)) {
                return;
            }
            if (Arrays.asList(Handlers.FILL.getFillModeItemList()).contains(level.getBlockState(blockPos).getBlock().asItem())) {
                workProgressFinishedCount++;
            }
        }

        if (isMineMode()) {
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Mine.MINE_SELECTION_TYPE)) {
                return;
            }
            if (level.getBlockState(blockPos).isAir()) {
                workProgressFinishedCount++;
            }
        }
        workProgressTotalCount++;
        workProgress = workProgressTotalCount < 1 ? workProgress : (float) workProgressFinishedCount / workProgressTotalCount;
    }
}
