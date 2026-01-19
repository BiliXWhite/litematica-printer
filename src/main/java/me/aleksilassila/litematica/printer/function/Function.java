package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.ModeType;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.MyBox;
import me.aleksilassila.litematica.printer.printer.Printer;

import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;


public abstract class Function extends PrinterUtils {
    public abstract PrintModeType getPrintModeType();

    public abstract ConfigBoolean getCurrentConfig();

    public AtomicReference<MyBox> multiBox = new AtomicReference<>();

    public void cooldownTick() {
    }

    public boolean isConfigAllowExecute(Printer printer) {
        ModeType modeType = (ModeType) Configs.General.WORK_MODE.getOptionListValue();
        if (modeType.equals(ModeType.SINGLE)) {
            PrintModeType printModeType = (PrintModeType) Configs.General.WORK_MODE_TYPE.getOptionListValue();
            if (!printModeType.equals(getPrintModeType())) {
                return false;
            }
        }
        if (modeType.equals(ModeType.MULTI)) {
            return getCurrentConfig().getBooleanValue();
        }
        return true;
    }

    public boolean canIterationTest(Printer printer, ClientLevel level, LocalPlayer player, BlockPos pos) {
        return true;
    }

    public @Nullable BlockPos getBoxBlockPos() {
        ModeType modeType = (ModeType) Configs.General.WORK_MODE.getOptionListValue();
        if (modeType.equals(ModeType.SINGLE)) {
            return Printer.getInstance().getBlockPos();
        }
        if (modeType.equals(ModeType.MULTI)) {
            return Printer.getInstance().getBlockPos(multiBox, this);
        }
        return null;
    }

    public abstract void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player);
}
