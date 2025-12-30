package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class FunctionMineMode extends FunctionModeBase {
    private final BreakManager breakManager = BreakManager.instance();
    private BlockPos breakPos = null;

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.MINE;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return InitHandler.MINE;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        BlockPos pos;
        while ((pos = breakPos == null ? printer.getBlockPos() : breakPos) != null) {
            if (PrinterUtils.isPositionInSelectionRange(player,pos,InitHandler.MINE_SELECTION_TYPE)) {
                if (BreakManager.breakRestriction(level.getBlockState(pos)) && breakManager.breakBlock(pos)) {
                    Printer.getInstance().requiredState = level.getBlockState(pos);
                    breakPos = pos;
                    return;
                }
            }
            breakPos = null;    // 一定要清空临时位置, 否则会进入死循环
        }
    }
}
