package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.options.ConfigBase;
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

    public GuiHandler() {
        super(NAME, null, Configs.Core.DEBUG_OUTPUT, null, true);
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        if (ConfigUtils.isPrintMode()) {
            WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
            if (schematic != null) {
                SchematicBlockContext context = new SchematicBlockContext(client, level, schematic, blockPos);
                if (!context.requiredState.isAir()) {
                    if (BlockPrintState.get(context) == BlockPrintState.CORRECT) {
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
            if (Arrays.asList(Handlers.FILL.getFillModeItemList()).contains(level.getBlockState(blockPos).getBlock().asItem())) {
                fillProgress.finished++;
                totalProgress.finished++; // 同步到总进度
            }
            fillProgress.total++;
            totalProgress.total++;
        }

        if (isMineMode()) {
            // 空气方块时，完成计数+1
            if (level.getBlockState(blockPos).isAir()) {
                mineProgress.finished++;
                totalProgress.finished++; // 同步到总进度
            }
            mineProgress.total++;
            totalProgress.total++;
        }
        printProgress.calculateProgress();
        fluidProgress.calculateProgress();
        fillProgress.calculateProgress();
        mineProgress.calculateProgress();
        totalProgress.calculateProgress();
    }

    @Override
    protected void stopIteration(boolean interrupt) {
        if (!interrupt) {
            resetAllProgress();
        }
    }

    /**
     * 重置所有模式的进度计数
     */
    private void resetAllProgress() {
        totalProgress.reset();
        printProgress.reset();
        fluidProgress.reset();
        fillProgress.reset();
        mineProgress.reset();
    }

    /**
     * 进度管理内部类（独立计数+自动修正进度范围）
     */
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