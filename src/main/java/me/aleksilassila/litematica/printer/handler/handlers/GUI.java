package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.options.ConfigBase;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockMatchingType;
import me.aleksilassila.litematica.printer.handler.Module;
import me.aleksilassila.litematica.printer.handler.ModuleManager;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.search.SearchSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GUI 统计处理器 — 增量遍历 box 计算进度，每 tick 分摊扫描量避免卡顿。
 */
public class GUI extends Module {
    public final static String NAME = "gui";
    private static final int SCAN_BUDGET_MS = 2;

    @Getter
    private final Progress totalProgress = new Progress(Configs.Print.ENABLED);
    @Getter
    private final Progress printProgress = new Progress(Configs.Print.ENABLED);
    @Getter
    private final Progress fluidProgress = new Progress(Configs.Fluid.ENABLED);
    @Getter
    private final Progress fillProgress = new Progress(Configs.Fill.ENABLED);
    @Getter
    private final Progress mineProgress = new Progress(Configs.Mine.ENABLED);

    private boolean scanning = false;

    public GUI() {
        super(NAME, Configs.Core.RENDER_HUD, null, true);
    }

    @Override
    protected boolean needsAreaCheck() {
        return false;
    }

    // GUI 统计模块自行在主线程分批扫描，不参与异步搜索
    @Override
    public @Nullable SearchSpec buildSearchSpec() {
        return null;
    }

    @Override
    protected boolean canExecute() {
        return false;
    }

    @Override
    public boolean canProcessPos(BlockPos pos) {
        return true;
    }

    @Override
    public boolean isCorrectBlock(BlockPos pos) {
        return true;
    }

    @Override
    protected void preprocess() {
        if (box == null || box.get() == null || level == null) return;

        if (!scanning) {
            startScan();
        }

        long deadline = System.currentTimeMillis() + SCAN_BUDGET_MS;
        BlockPos pos;
        while ((pos = iteratorManager.next()) != null) {
            countPosition(pos);
            if (System.currentTimeMillis() >= deadline) return;
        }

        finishScan();
    }

    private void startScan() {
        scanning = true;
        totalProgress.resetCounters();
        printProgress.resetCounters();
        fluidProgress.resetCounters();
        fillProgress.resetCounters();
        mineProgress.resetCounters();
        iteratorManager.reset();
    }

    private void finishScan() {
        scanning = false;
        printProgress.calculateProgress();
        fluidProgress.calculateProgress();
        fillProgress.calculateProgress();
        mineProgress.calculateProgress();
        totalProgress.calculateProgress();
    }

    private void countPosition(BlockPos blockPos) {
        if (Configs.Print.ENABLED.getBooleanValue()) {
            WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
            if (schematic != null) {
                SchematicBlockContext context = new SchematicBlockContext(mc, level, schematic, blockPos);
                if (!context.requiredState.isAir()) {
                    if (BlockMatchingType.get(context) == BlockMatchingType.CORRECT) {
                        printProgress.finished++;
                        totalProgress.finished++;
                    }
                    printProgress.total++;
                    totalProgress.total++;
                }
            }
        }
        if (Configs.Fluid.ENABLED.getBooleanValue()) {
            if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
                fluidProgress.finished++;
                totalProgress.finished++;
            }
            fluidProgress.total++;
            totalProgress.total++;
        }
        if (Configs.Fill.ENABLED.getBooleanValue()) {
            if (Arrays.asList(ModuleManager.FILL.getFillModeItemList()).contains(level.getBlockState(blockPos).getBlock().asItem())) {
                fillProgress.finished++;
                totalProgress.finished++;
            }
            fillProgress.total++;
            totalProgress.total++;
        }
        if (Configs.Mine.ENABLED.getBooleanValue()) {
            if (level.getBlockState(blockPos).isAir()) {
                mineProgress.finished++;
                totalProgress.finished++;
            }
            mineProgress.total++;
            totalProgress.total++;
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

        public void resetCounters() {
            this.total = 0;
            this.finished = 0;
        }

        public void calculateProgress() {
            progress = total < 1 ? lastProgress : (float) finished / total;
            lastProgress = progress;
        }
    }
}