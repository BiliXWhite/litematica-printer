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
        // 本次TICK共享部分预先检查
        if (isOpenHandler || switchItem() || InteractionUtils.INSTANCE.hasTargets()) {
            return;
        }
        if (ActionManager.INSTANCE.sendQueue(mc.player).needWaitModifyLook) {
            return;
        }
        if (Configs.Core.LAG_CHECK.getBooleanValue()) {
            if (packetTick > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
                return;
            }
            packetTick++;
        }
        for (ClientPlayerTickHandler handler : VALUES) {
            // 同TICK不同处理程序进行二次迭代检查, 避免独立的处理程序修改了内容没有及时跳出导致出现资源抢占问题
            if (isOpenHandler || switchItem() || InteractionUtils.INSTANCE.hasTargets()) {
                return;
            }
            // 有任务需要修改视角强制退出
            if (ActionManager.INSTANCE.needWaitModifyLook) {
                return;
            }
            handler.tick();
        }
    }

    public static void updateTickHandlerTime() {
        currentHandlerTime++;
    }
}
