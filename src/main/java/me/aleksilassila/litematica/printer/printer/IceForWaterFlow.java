package me.aleksilassila.litematica.printer.printer;

/**
 * 破冰放水流状态机的纯决策逻辑（无 Minecraft 依赖，可单元测试）。
 * <p>
 * executeIteration 与 PlacementGuide.buildAction 只负责副作用（放置/挖掘/等待），
 * 每一步该做什么由这里根据原始状态决定。
 * </p>
 */
public final class IceForWaterFlow {
    private IceForWaterFlow() {}

    /** executeIteration 决策结果 */
    public enum Step {
        /** 当前无冰无水 → 放置冰块 */
        PLACE_ICE,
        /** 冰已放置 → 入队挖掘并进入等待 */
        BREAK_ICE_AND_WAIT,
        /** 正在等待水生成，未超时 → 继续等待 */
        KEEP_WAITING,
        /** 等待超过 maxWaitTicks 仍无水 → 清除标记，由调用方转入冷却退避，避免无限循环 */
        WAIT_TIMEOUT,
        /** 目标位置已有纯水源（或水刚生成）→ 放置含水方块（清除等待） */
        PLACE_BLOCK,
        /** 功能未开启 → 不处理 */
        SKIP
    }

    public static Step decide(boolean enabled, boolean isWaitingHere, boolean isIce,
                              boolean matchesWaterRequest, int waitTicks, int maxWaitTicks) {
        if (!enabled) return Step.SKIP;
        if (isWaitingHere) {
            if (matchesWaterRequest) return Step.PLACE_BLOCK;
            return waitTicks > maxWaitTicks ? Step.WAIT_TIMEOUT : Step.KEEP_WAITING;
        }
        if (isIce) return Step.BREAK_ICE_AND_WAIT;
        return matchesWaterRequest ? Step.PLACE_BLOCK : Step.PLACE_ICE;
    }

    /** PlacementGuide.buildAction 决策结果 */
    public enum BuildDecision {
        /** 走正常的状态处理（MISSING/ERROR_BLOCK 等） */
        NORMAL,
        /** 需要水且空位 → 返回冰块放置 Action */
        PLACE_ICE,
        /** 目标位置已有纯水源 → 返回放置含水方块 Action（直接放入水中） */
        PLACE_BLOCK,
        /** 当前是冰块 → 入队挖掘并返回占位 Action（保证 canProcessPos 通过） */
        QUEUE_ICE_BREAK,
        /** 下方禁止/无法放冰 → 跳过 */
        SKIP
    }

    public static BuildDecision decideBuildAction(boolean featureEnabled, boolean matchesWaterRequest,
            boolean isIce, boolean stateIsMissing, boolean belowForbidden, boolean isSurvivalMode) {
        if (!featureEnabled) return BuildDecision.NORMAL;
        if (matchesWaterRequest) return BuildDecision.PLACE_BLOCK;
        if (isIce) return BuildDecision.QUEUE_ICE_BREAK;
        if (stateIsMissing) {
            if (!isSurvivalMode || belowForbidden) return BuildDecision.SKIP;
            return BuildDecision.PLACE_ICE;
        }
        return BuildDecision.NORMAL;
    }
}
