package me.aleksilassila.litematica.printer.mixin_extension;

// 方块破坏结果枚举（核心新增）
public enum BlockBreakResult {
    COMPLETED,    // 破坏完成
    IN_PROGRESS,  // 正在破坏，需要继续tick
    ABORTED,      // 破坏被中止（切换方块等）
    FAILED        // 破坏失败（无权限/超出边界/无法交互等）
}
