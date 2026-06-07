package me.aleksilassila.litematica.printer.enums;

/**
 * 扫描状态机：控制迭代扫描的粒度和惰性行为。
 * <p>
 * FULL    — 全量扫描整个 PrinterBox（默认行为，积积极响应）
 * PARTIAL — 只扫描 RegionTracker 中标记为 DIRTY 的子区块
 * LAZY    — 跳过迭代扫描，仅通过 OperationQueue 处理 BlockUpdate 修复
 * </p>
 */
public enum ScanState {
    FULL,
    PARTIAL,
    LAZY
}
