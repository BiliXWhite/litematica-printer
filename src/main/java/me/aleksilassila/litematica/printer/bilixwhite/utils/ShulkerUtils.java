package me.aleksilassila.litematica.printer.bilixwhite.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.QuickShulkerModeType;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class ShulkerUtils {
    static final Minecraft client = Minecraft.getInstance();
    static IConfigOptionListEntry openMode = Configs.QUICK_SHULKER_MODE.getOptionListValue();

    public static void openShulker(ItemStack stack, int shulkerBoxSlot) {
        if (openMode == QuickShulkerModeType.CLICK_SLOT) {
            client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, shulkerBoxSlot, 1, ClickType.PICKUP, client.player);
        } else if (openMode == QuickShulkerModeType.INVOKE) {
            if (ModLoadStatus.isLoadQuickShulkerLoaded()) {
                try {
                    ClientUtil.CheckAndSend(stack, shulkerBoxSlot);
                } catch (Exception ignored) {}
            } else MessageUtils.addMessage(StringUtils.literal("快捷潜影盒模组未加载！"));
        }
    }
}
