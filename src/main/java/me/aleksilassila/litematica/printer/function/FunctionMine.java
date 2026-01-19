package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FunctionMine extends Function {
    private final BreakManager breakManager = BreakManager.instance();
    private Map<BlockPos, Integer> cooldownList = new HashMap<>();
    private BlockPos blockPos = null;

    // 新增：缓存最近处理的挖矿方块（保留1个Tick，供HUD显示）
    private BlockPos lastMinedBlock = null;
    // 新增：记录单个Tick内处理的方块数量
    private int tickMinedCount = 0;
    // 新增：缓存清理计数器（确保数据至少保留1个Tick）
    private int lastBlockCacheTick = 0;

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.MINE;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.Excavate.MINE;
    }

    // 新增：获取最近处理的方块（供HUD调用）
    public BlockPos getLastMinedBlock() {
        return lastMinedBlock;
    }

    // 新增：获取单Tick处理数量（供HUD调用）
    public int getTickMinedCount() {
        return tickMinedCount;
    }

    public BlockPos getBlockPos() {
        // 优先返回当前处理的方块，无则返回最近处理的方块
        return blockPos != null ? blockPos : lastMinedBlock;
    }

    @Override
    public void cooldownTick() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = cooldownList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int newValue = entry.getValue() - 1;
            if (newValue <= 0) {
                iterator.remove();
            } else {
                entry.setValue(newValue);
            }
        }

        // 新增：缓存清理逻辑（每Tick递减，至少保留1个Tick）
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
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        // 重置单Tick计数（每个Tick开始时清零）
        tickMinedCount = 0;
        boolean loop = true;
        while (loop && (blockPos = (blockPos == null) ? printer.getBlockPos() : blockPos) != null) {
            if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0 && printer.printerWorkingCountPerTick == 0) {
                loop = false;
                continue;
            }
            if (cooldownList.containsKey(blockPos)) {
                this.blockPos = null;
                continue;
            }
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Excavate.MINE_SELECTION_TYPE)) {
                this.blockPos = null;
                continue;
            }
            if (level.getBlockState(blockPos).isAir()) {
                this.blockPos = null;
                continue;
            }
            if (!BreakManager.breakRestriction(level.getBlockState(blockPos))) {
                this.blockPos = null;
                continue;
            }

            boolean localPrediction = !Configs.General.BREAK_PLACE_USE_PACKET.getBooleanValue();
            InteractionUtils.BlockBreakResult result = InteractionUtils.INSTANCE.updateBlockBreakingProgress(blockPos, Direction.DOWN, localPrediction);

            // 处理成功（瞬间破坏）
            if (result != InteractionUtils.BlockBreakResult.IN_PROGRESS) {
                // 缓存最近处理的方块，设置缓存时间（10个Tick，确保HUD能渲染到）
                lastMinedBlock = blockPos;
                lastBlockCacheTick = 10;
                // 累加单 Tick 处理数量
                tickMinedCount++;
                cooldownList.put(blockPos, 8);
                this.blockPos = null;
            } else {
                // 正在破坏中，保留blockPos
                return;
            }

            if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0) {
                printer.printerWorkingCountPerTick--;
            }
        }
    }
}