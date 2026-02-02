package me.aleksilassila.litematica.printer.function.breaks;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockCooldownType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.function.FunctionBreak;
import me.aleksilassila.litematica.printer.printer.BlockCooldownManager;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class FunctionMine extends FunctionBreak {
    private BlockPos blockPos = null;
    private @Getter BlockPos lastMinedBlock = null; // 缓存最近处理的挖矿方块（保留1个Tick，供HUD显示）
    private @Getter int tickMinedCount = 0;         // 记录单个Tick内处理的方块数量
    private int lastBlockCacheTick = 0;             // 缓存清理计数器（确保数据至少保留1个Tick）

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.MINE;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.Core.MINE;
    }

    public BlockPos getBlockPos() {
        return blockPos != null ? blockPos : lastMinedBlock;
    }

    @Override
    public boolean canIterationTest(Printer printer, ClientLevel level, LocalPlayer player, BlockPos pos) {
        if (pos != null) {
            if (!PrinterUtils.isPositionInSelectionRange(player, pos, Configs.Mine.MINE_SELECTION_TYPE)) {
                return false;
            }
            if (BlockCooldownManager.INSTANCE.isOnCooldown(BlockCooldownType.MINE, blockPos)) {
                return false;
            }
            return InteractionUtils.canBreakBlock(pos) && InteractionUtils.breakRestriction(level.getBlockState(pos));
        }
        return true;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        tickMinedCount = 0; // 重置单Tick计数（每个Tick开始时清零）
        int breakBlocksPerTick = Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue();
        boolean loop = true;
        while (loop && (blockPos = blockPos == null ? getBoxBlockPos() : blockPos) != null) {
            if (BlockCooldownManager.INSTANCE.isOnCooldown(BlockCooldownType.MINE, blockPos)) {
                loop = false;
                this.blockPos = null;
                continue;
            }
            if (Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue() != 0 && breakBlocksPerTick < 0) {
                loop = false;
            }
            if (!canIterationTest(printer, level, client.player, blockPos)) {
                this.blockPos = null;
                continue;
            }
            InteractionUtils.BlockBreakResult result = InteractionUtils.INSTANCE.continueDestroyBlock(blockPos);
            lastMinedBlock = blockPos;  // 缓存最近处理的方块，设置缓存时间（10个Tick，确保HUD能渲染到）
            lastBlockCacheTick = 10;    // 累加单 Tick 处理数量
            tickMinedCount++;
            if (result == InteractionUtils.BlockBreakResult.IN_PROGRESS) {
                return;
            }
            BlockCooldownManager.INSTANCE.setCooldown(BlockCooldownType.MINE, blockPos, ConfigUtils.getBreakCooldown());
            this.blockPos = null;
            if (Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue() != 0) {
                breakBlocksPerTick--;
            }
        }
    }
}