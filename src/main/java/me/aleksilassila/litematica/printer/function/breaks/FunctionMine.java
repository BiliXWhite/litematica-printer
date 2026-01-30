package me.aleksilassila.litematica.printer.function.breaks;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import lombok.Getter;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.function.FunctionBreak;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
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
        return Configs.Excavate.MINE;
    }

    public BlockPos getBlockPos() {
        return blockPos != null ? blockPos : lastMinedBlock;
    }

    @Override
    public void cooldownTick() {
        super.cooldownTick();
        // 缓存清理逻辑（每Tick递减，至少保留1个Tick）
        if (lastBlockCacheTick > 0) {
            lastBlockCacheTick--;
            // 缓存过期后清空，避免内存泄漏
            if (lastBlockCacheTick == 0) {
                lastMinedBlock = null;
                tickMinedCount = 0;
            }
        }
    }

    @Override
    public boolean canIterationTest(Printer printer, ClientLevel level, LocalPlayer player, BlockPos pos) {
        if (pos != null) {
            if (!PrinterUtils.isPositionInSelectionRange(player, pos, Configs.Excavate.MINE_SELECTION_TYPE)) {
                return false;
            }
            if (isBreakCooldown(pos)) {
                return false;
            }
            return InteractionUtils.canBreakBlock(pos);
        }
        return true;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        if (InteractionUtils.INSTANCE.isDestroying()) {
            return;
        } else {
            blockPos = null;
        }
        tickMinedCount = 0; // 重置单Tick计数（每个Tick开始时清零）
        int breakBlocksPerTick = Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue();
        boolean loop = true;
        while (loop && (blockPos = blockPos == null ? getBoxBlockPos() : blockPos) != null) {
            if (isBreakCooldown(blockPos)) {
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
            InteractionUtils.BlockBreakResult result = InteractionUtils.INSTANCE.updateBlockBreakingProgress(blockPos);
            lastMinedBlock = blockPos;  // 缓存最近处理的方块，设置缓存时间（10个Tick，确保HUD能渲染到）
            lastBlockCacheTick = 10;    // 累加单 Tick 处理数量
            tickMinedCount++;
            if (result == InteractionUtils.BlockBreakResult.IN_PROGRESS) {
                return;
            }
            setBreakCooldown(blockPos); // 仅在瞬间破坏成功中设置冷却, 避免持续破坏位置被冷却
            this.blockPos = null;
            if (Configs.Break.BREAK_BLOCKS_PER_TICK.getIntegerValue() != 0) {
                breakBlocksPerTick--;
            }
        }
    }
}