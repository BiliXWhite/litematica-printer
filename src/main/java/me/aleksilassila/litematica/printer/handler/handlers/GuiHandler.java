package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.options.ConfigBase;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class GuiHandler extends ClientPlayerTickHandler {
    public final static String NAME = "gui";

    @Getter
    private final Progress totalProgress = new Progress(Configs.Core.PRINT);
    @Getter
    private final Progress printProgress = new Progress(Configs.Core.PRINT);
    @Getter
    private final Progress fluidProgress = new Progress(Configs.Core.FLUID);
    @Getter
    private final Progress fillProgress = new Progress(Configs.Core.FILL);
    @Getter
    private final Progress mineProgress = new Progress(Configs.Core.MINE);

    private final Progress[] progresses = new Progress[]{totalProgress, printProgress, fluidProgress, fillProgress, mineProgress};

    public GuiHandler() {
        super(NAME, null, Configs.Core.RENDER_HUD, null, true);
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        if (ConfigUtils.isPrintMode()) {
            WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
            if (schematic != null) {
                SchematicBlockContext context = new SchematicBlockContext(client, level, schematic, blockPos);
                if (!context.requiredState.isAir()) {
                    if (BlockMatchResult.compare(context) == BlockMatchResult.CORRECT) {
                        printProgress.finished++;
                        totalProgress.finished++;
                    }
                    printProgress.total++;
                    totalProgress.total++;
                }
            }
        }
        if (isFluidMode()) {
            if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
                fluidProgress.finished++;
                totalProgress.finished++;
            }
            fluidProgress.total++;
            totalProgress.total++;
        }
        if (isFillMode()) {
            if (!level.getBlockState(blockPos).isAir()) {
                fillProgress.finished++;
                totalProgress.finished++;
            }
            fillProgress.total++;
            totalProgress.total++;
        }
        if (isMineMode()) {
            if (level.getBlockState(blockPos).isAir()) {
                mineProgress.finished++;
                totalProgress.finished++;
            }
            mineProgress.total++;
            totalProgress.total++;
        }
        for (Progress progress : progresses) {
            progress.calculateProgress();
        }
    }

    @Override
    protected void stopIteration(boolean interrupt) {
        if (!interrupt) {
            for (Progress progress : progresses) {
                progress.reset();
            }
        }
    }

    @Getter
    public static class Progress {
        private final ConfigBase<?> config;
        private long total;
        private long finished;
        private double progress;
        private double lastProgress;

        public Progress(ConfigBase<?> config) {
            this.config = config;
            this.total = 0;
            this.finished = 0;
            this.progress = 0.0;
        }

        public double getProgress() {
            return progress <= 0 ? lastProgress : progress;
        }

        public void calculateProgress() {
            progress = total < 1 ? lastProgress : (float) finished / total;
            lastProgress = progress;
        }

        public void reset() {
            this.total = 0;
            this.finished = 0;
            this.progress = 0.0;
        }
    }
}