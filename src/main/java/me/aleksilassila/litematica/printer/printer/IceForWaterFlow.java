package me.aleksilassila.litematica.printer.printer;

/**
 * 破冰放水流状态机的纯决策逻辑（无 Minecraft 依赖，可单元测试）。
 * <p>
 * 调用方只负责副作用（放置/挖掘/等待），每一步该做什么由这里根据原始状态决定。
 * 注意：调用前需确保「破冰放水」功能已开启。
 * </p>
 */
public final class IceForWaterFlow {
    private IceForWaterFlow() {}

    public enum Step {
        /** 当前无冰无水 → 放置冰块 */
        PLACE_ICE,
        /** 冰已放置 → 入队挖掘并进入等待 */
        BREAK_ICE_AND_WAIT,
        /** 正在等待水生成，未超时 → 继续等待 */
        KEEP_WAITING,
        /** 等待超过 maxWaitTicks 仍无水 → 清除标记并转入冷却退避，避免无限循环 */
        WAIT_TIMEOUT,
        /** 目标位置已有水源（或水刚生成）→ 放置含水方块（清除等待） */
        PLACE_BLOCK
    }

    public static Step decide(boolean isWaitingHere, boolean isIce,
                              boolean waterReady, int waitTicks, int maxWaitTicks) {
        if (isWaitingHere) {
            if (waterReady) return Step.PLACE_BLOCK;
            return waitTicks > maxWaitTicks ? Step.WAIT_TIMEOUT : Step.KEEP_WAITING;
        }
        if (isIce) return Step.BREAK_ICE_AND_WAIT;
        return waterReady ? Step.PLACE_BLOCK : Step.PLACE_ICE;
    }
}
