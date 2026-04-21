package me.aleksilassila.litematica.printer.utils.mods;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.QuickShulkerModeType;
import me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings({"DataFlowIssue", "SpellCheckingInspection"})
public class ShulkerUtils {
    static final Minecraft client = Minecraft.getInstance();
    static IConfigOptionListEntry openMode = Configs.Placement.QUICK_SHULKER_MODE.getOptionListValue();

    public static void openShulker(ItemStack stack, int shulkerBoxSlot) {
        if (openMode == QuickShulkerModeType.CLICK_SLOT) {
            client.gameMode.handleContainerInput(client.player.containerMenu.containerId, shulkerBoxSlot, 1, ContainerInput.PICKUP, client.player);
        } else if (openMode == QuickShulkerModeType.INVOKE) {
            if (ModLoadUtils.isQuickShulkerLoaded()) {
                try {
                    ClientUtil.CheckAndSend(stack, shulkerBoxSlot);
                } catch (Exception ignored) {}
            } else MessageUtils.addMessage("快捷潜影盒模组未加载！");
        }
    }
}
