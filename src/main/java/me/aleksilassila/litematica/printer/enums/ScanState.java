package me.aleksilassila.litematica.printer.enums;

/**
 * 扫描状态：控制迭代扫描的阶段。
 * <p>
 * RUNNING — 正常单遍扫描：每 tick 从迭代器取下一个坐标依次处理
 * WAITING — 暂停迭代，等待异步条件满足后继续
 * </p>
 */
public enum ScanState {
    RUNNING,
    WAITING
}