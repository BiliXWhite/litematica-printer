package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class FunctionBedrockMode extends FunctionModeBase {

    @Override
    public State.PrintModeType getPrintModeType() {
        return State.PrintModeType.BEDROCK;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return InitHandler.BEDROCK;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        printer.printerYAxisReverse = true;
        if (player.isCreative()) {
            ZxyUtils.actionBar("创造模式无法使用破基岩模式！");
            return;
        }
        if (!Statistics.loadBedrockMiner) {
            ZxyUtils.actionBar("未安装Bedrock Miner模组/游戏版本小于1.19，无法破基岩！");
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
            if (!PlaceUtils.canInteracted(pos) || PrinterUtils.isLimitedByTheNumberOfLayers(pos) || !Printer.TempData.xuanQuFanWeiNei_p(pos)) {
                continue;
            }
            BedrockUtils.addToBreakList(pos, client.level);
            // 原谅我使用硬编码plz 我真的不想写太多的优化了555
            Printer.getInstance().placeCooldownList.put(pos, 100);
        }
    }
}
