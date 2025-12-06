package me.aleksilassila.litematica.printer.bilixwhite.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class ShulkerUtils {
    static final Minecraft client = Minecraft.getInstance();
    static IConfigOptionListEntry openMode = InitHandler.QUICK_SHULKER_MODE.getOptionListValue();

    public static void openShulker(ItemStack stack, int shulkerBoxSlot) {
        if (openMode == State.QuickShulkerModeType.CLICK_SLOT) {
            client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, shulkerBoxSlot, 1, ClickType.PICKUP, client.player);
        } else if (openMode == State.QuickShulkerModeType.INVOKE) {
            if (Statistics.loadQuickShulker) {
                try {
                    ClientUtil.CheckAndSend(stack, shulkerBoxSlot);
                } catch (Exception ignored) {}
            } else StringUtils.printChatMessage("快捷潜影盒模组未加载！");
        }
    }
}
