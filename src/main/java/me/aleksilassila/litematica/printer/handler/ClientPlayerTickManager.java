package me.aleksilassila.litematica.printer.handler;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.handlers.*;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.client.Minecraft;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

@SuppressWarnings("SpellCheckingInspection")
public class ClientPlayerTickManager {
    public static final Minecraft mc = Minecraft.getInstance();

    public static final GuiHandler GUI = new GuiHandler();
    public static final PrintHandler PRINT = new PrintHandler();
    public static final FillHandler FILL = new FillHandler();
    public static final MineHandler MINE = new MineHandler();
    public static final FluidHandler FLUID = new FluidHandler();
    public static final BedrockHandler BEDROCK = new BedrockHandler();

    @Getter
    @Setter
    private static int packetTick;
    @Getter
    private static long currentHandlerTime;

    public static final ImmutableList<ClientPlayerTickHandler> VALUES = ImmutableList.of(
            GUI, PRINT, FILL, FLUID, MINE, BEDROCK
    );

    public static void tick() {
        if (Configs.Core.LAG_CHECK.getBooleanValue()) {
            if (packetTick > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
                return;
            }
            packetTick++;
        }
        boolean inventoryBusy = isOpenHandler || switchItem();
        if (!inventoryBusy && mc.player != null) {
            ActionManager.INSTANCE.sendQueue(mc.player);
        }
        for (ClientPlayerTickHandler handler : VALUES) {
            if (handler.shouldPauseForInventoryActivity() && inventoryBusy) {
                continue;
            }
            if (handler.shouldPauseForInteractionQueue() && InteractionUtils.INSTANCE.isNeedHandle()) {
                continue;
            }
            if (handler.shouldPauseForActionQueue() && ActionManager.INSTANCE.isBusy()) {
                continue;
            }
            handler.tick();
            inventoryBusy = isOpenHandler || switchItem();
            if (!inventoryBusy && mc.player != null) {
                ActionManager.INSTANCE.sendQueue(mc.player);
            }
        }
    }

    public static void updateTickHandlerTime() {
        currentHandlerTime++;
    }
}
