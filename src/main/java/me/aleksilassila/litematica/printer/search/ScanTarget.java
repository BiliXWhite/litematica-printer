package me.aleksilassila.litematica.printer.search;

import net.minecraft.core.BlockPos;

// 由 Module 实现，两个方法会在搜索工作线程上高频调用，必须是只读的
public interface ScanTarget {
    // 粗筛，通常是 !isCorrectBlock(pos)
    boolean isCandidatePos(BlockPos pos);

    // 该位置是否在当前工作区内
    boolean isInWorkspace(BlockPos pos);
}
