package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class FunctionMineMode extends FunctionMode {
    public final BreakManager breakManager = BreakManager.instance();
    BlockPos breakPos = null;

    @Override
    public State.PrintModeType getPrintModeType() {
        return State.PrintModeType.MINE;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return InitHandler.MINE;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        BlockPos pos;
        while ((pos = breakPos == null ? printer.getBlockPos() : breakPos) != null) {
            if (BreakManager.breakRestriction(client.level.getBlockState(pos)) && breakManager.breakBlock(pos)) {
                Printer.getInstance().requiredState = client.level.getBlockState(pos);
                breakPos = pos;
                return;
            }
            // 清空临时位置
            breakPos = null;
        }
    }
}
