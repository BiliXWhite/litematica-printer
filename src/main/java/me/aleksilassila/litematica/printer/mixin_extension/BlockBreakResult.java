package me.aleksilassila.litematica.printer.mixin_extension;

// 方块破坏结果枚举（核心新增）
public enum BlockBreakResult {
    COMPLETED,    // 破坏完成
    COMPLETED_WAIT, // 破坏完成（但是不能再统一tick内完成多次）
    IN_PROGRESS,  // 正在破坏，需要继续tick
    FAILED        // 破坏失败（无权限/超出边界/无法交互等）
}
