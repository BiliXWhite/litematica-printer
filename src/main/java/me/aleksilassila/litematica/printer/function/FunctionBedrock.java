package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class FunctionBedrock extends Function {

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.BEDROCK;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.Hotkeys.BEDROCK;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        if (player.isCreative()) {
            MessageUtils.setOverlayMessage("创造模式无法使用破基岩模式！");
            return;
        }
        if (!ModLoadStatus.isBedrockMinerLoaded()) {
            MessageUtils.setOverlayMessage("未安装 Fabric-Bedrock-Miner 模组/游戏版本小于1.19，无法破基岩！");
            return;
        }
        if (!BedrockUtils.isWorking()) {
            BedrockUtils.setWorking(true);
        }
        if (BedrockUtils.isBedrockMinerFeatureEnable()) {   // 限制原功能(手动点击或使用方块：添加、开关)
            BedrockUtils.setBedrockMinerFeatureEnable(false);
        }
        BlockPos pos;
        while ((pos = printer.getBlockPos()) != null) {
            BedrockUtils.addToBreakList(pos, client.level);
            Printer.getInstance().placeCooldownList.put(pos, 100);
        }
    }
}
