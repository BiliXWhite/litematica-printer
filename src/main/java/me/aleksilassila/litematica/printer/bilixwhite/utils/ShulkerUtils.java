package me.aleksilassila.litematica.printer.bilixwhite.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class ShulkerUtils {
    static final MinecraftClient client = MinecraftClient.getInstance();
    static IConfigOptionListEntry openMode = LitematicaMixinMod.QUICK_SHULKER_MODE.getOptionListValue();

    public static void openShulker(ItemStack stack, int shulkerBoxSlot) {
        if (openMode == State.QuickShulkerModeType.CLICK_SLOT) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, shulkerBoxSlot, 1, SlotActionType.PICKUP, client.player);
        } else if (openMode == State.QuickShulkerModeType.INVOKE) {
            if (Statistics.loadQuickShulker) {
                try {
                    ClientUtil.CheckAndSend(stack, shulkerBoxSlot);
                } catch (Exception ignored) {}
            } else StringUtils.printChatMessage("快捷潜影盒模组未加载！");
        }
    }
}
