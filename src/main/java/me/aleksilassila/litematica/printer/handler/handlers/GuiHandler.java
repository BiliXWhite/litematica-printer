package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockPrintState;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.Handlers;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class GuiHandler extends ClientPlayerTickHandler {
    public final static String NAME = "gui";

    private long workProgressTotalCount;
    private long workProgressFinishedCount;
    @Getter
    private double workProgress;

    public GuiHandler() {
        super(NAME, null, Configs.Core.DEBUG_OUTPUT, null, true);
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        if (ConfigUtils.isPrintMode()) {
            WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
            if (schematic == null) {
                workProgress = 0.0f;
                return;
            }
            SchematicBlockContext context = new SchematicBlockContext(client, level, schematic, blockPos);
            if (context.requiredState.isAir()) {
                return;
            }
            if (BlockPrintState.get(context) == BlockPrintState.CORRECT) {
                workProgressFinishedCount++;
            }
        }
        if (isFluidMode()) {
            if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
                workProgressFinishedCount++;
            }
        }
        if (isFillMode()) {
            if (Arrays.asList(Handlers.FILL.getFillModeItemList()).contains(level.getBlockState(blockPos).getBlock().asItem())) {
                workProgressFinishedCount++;
            }
        }
        if (isMineMode()) {
            if (level.getBlockState(blockPos).isAir()) {
                workProgressFinishedCount++;
            }
        }
        workProgressTotalCount++;
    }

    @Override
    protected void stopIteration(boolean interrupt) {
        workProgress = workProgressTotalCount < 1 ? workProgress : (float) workProgressFinishedCount / workProgressTotalCount;
        if (!interrupt) {
            workProgressTotalCount = 0;
            workProgressFinishedCount = 0;
        }
    }
}
