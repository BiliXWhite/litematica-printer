package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import org.jetbrains.annotations.NotNull;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;

public class FunctionBedrockMode extends FunctionModeBase {

    public BlockPos pos1;
    public BlockPos pos2;

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
        if (!ModLoadStatus.isBedrockMinerLoaded()) {
            ZxyUtils.actionBar("未安装 Fabric-Bedrock-Miner 模组/游戏版本小于1.19，无法破基岩！");
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
            if (PrinterUtils.isLimitedByTheNumberOfLayers(pos))
                continue;
            if (!Printer.TempData.xuanQuFanWeiNei_p(pos))
                continue;
            // 跳过冷却中的位置
            if (Printer.getInstance().placeCooldownList.containsKey(pos))
                continue;
            Printer.getInstance().placeCooldownList.put(pos, 100);
            BedrockUtils.addToBreakList(pos, client.level);
        }
    }
}
