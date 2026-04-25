package me.aleksilassila.litematica.printer.handler;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.handlers.*;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.BreakUtils;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import net.minecraft.client.Minecraft;

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
        if (InventoryUtils.isOpenHandler || InventoryUtils.switchItem() || BreakUtils.INSTANCE.isNeedHandle()) {
            return;
        }
        
        // 检查是否需要等待视角修改
        if (ActionManager.INSTANCE.sendQueue(mc.player).needWaitModifyLook) {
            return;
        }

        // 延迟检查
        if (Configs.Core.LAG_CHECK.getBooleanValue()) {
            if (packetTick > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
                return;
            }
            packetTick++;
        }

        // 遍历所有处理器执行tick逻辑
        for (ClientPlayerTickHandler handler : VALUES) {
            // 非GUI处理器需要进行二次迭代检查，避免资源抢占问题
            if (!(handler instanceof GuiHandler)) {
                if (InventoryUtils.isOpenHandler || InventoryUtils.switchItem() || BreakUtils.INSTANCE.isNeedHandle()) {
                    return;
                }
                // 有任务需要修改视角时强制退出
                if (ActionManager.INSTANCE.needWaitModifyLook) {
                    return;
                }
            }
            handler.tick();
        }
    }

    public static void updateTickHandlerTime() {
        currentHandlerTime++;
    }
}